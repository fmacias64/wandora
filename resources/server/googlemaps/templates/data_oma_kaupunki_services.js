## Helper macros
## Escapes quotation marks inside strings
#macro( ok_plain_text $arg )##
#if( ! $arg )#set( $arg = "" )#end##
#set( $ptxt = $arg.replaceAll('"', ' ') )##
#set( $ptxt = $ptxt.replace('[\p{Cntrl}\p{Space}]', ' ') )##
"$ptxt"##
#end##

#set( $okServiceSI = "http://omakaupunki.fi/service/") ##
#set( $okServiceItem = $topicmap.getTopic( $okServiceSI ) )##

#set( $okLocationSi = "http://www.geonames.org/coordinates" )##
#set( $okLocationItem = $topicmap.getTopic( $okLocationSi ) )##

#if( $okLocationItem )##
    #set( $ok_services = $topicmap.getTopicsOfType( $okServiceItem ) )##
#end##

## Limit for processed topics, set 0 or less for no limit.
#set( $count_limit = 0 )##

## Extract data need for Google Maps
var ok_service_locations = [
#if( $ok_services && $ok_services.size()!=0 )##
	#foreach( $service in $ok_services )##
	{
		#set( $location = $service.getData( $okLocationItem, $lang ) )##
		#set ( $title = $service.getDisplayName($lang) )##
		title: #ok_plain_text($title),
		location: #ok_plain_text( $location ),
		#set( $as_map = $mapmaker.make() )## Hashmap for associations
		#set( $associations = $service.getAssociations() )##
		#foreach( $layer in $associations )##
			#set( $type_title = $layer.getType().getDisplayName($lang) )##
			#if(!$as_map.containsKey($type_title) )##
				#set($temp = $as_map.put($type_title, []))##
			#end##
			#set($vals = $as_map.get($type_title))##
			#set( $roles = $layer.getRoles() )##
			#foreach($role in $roles) ##
				#set( $player = $layer.getPlayer($role) )##
				#if(!$tmbox.isTopicOfType($player, $okServiceItem) )##
					#set( $player_title = $player.getDisplayName($lang) )##
					#set($temp = $vals.add($player_title))##
				#end ##
			#end ##
			#set($temp = $as_map.put($type_title, $vals) ) ##
		#end##
		
		extra_data: [
		#set( $data_types = $service.getDataTypes() )##
		#if( $data_types.size()!=0 )##
			#foreach( $type in $data_types )##
				#if($type.getOneSubjectIdentifier().toExternalForm() != $okLocationSi)##
				{
				name:#ok_plain_text($type.getDisplayName($lang) ),
				values:[#ok_plain_text( $service.getData($type, $lang) )]
				},
				#end##
			#end##
		#end##
		#foreach ($entry in $as_map.entrySet())
		{
			name: #ok_plain_text($entry.key),
			values: [
			#foreach($value in $entry.value)##
			#ok_plain_text($value),
			#end##
			]
		},
		#end
		],
	},
		#set( $count_limit = $count_limit - 1 )##
		#if($count_limit == 0)##
			#break##
		#end##
		
	
	#end##
#end##

];

## Create markers for Google Maps from extracted data
for(var i=0;i<ok_service_locations.length;i++) {

	var raw_latlng = ok_service_locations[i].location.split(",");
	var latlng = new google.maps.LatLng(raw_latlng[0],raw_latlng[1]);
	
	var loc_data = ok_service_locations[i];
	
	coordinates.push(latlng);
	
	## Marker popup content
	var info_html = '<div id="info-content">';
	info_html = "<h2>"+loc_data.title+"</h2>";
	if(loc_data.extra_data.length > 0) {
		info_html += '<ul id="statistics">';
		for(var j=0;j<loc_data.extra_data.length;j++) {
			
			if(loc_data.extra_data[j].values.length > 1) {
				info_html += "<li><strong>" + loc_data.extra_data[j].name + "</strong></li>";
				info_html += '<ul>';
				for(var k=0;k<loc_data.extra_data[j].values.length;k++) {
					info_html += "<li>" + loc_data.extra_data[j].values[k] + "</li>";
				}
				info_html += '</ul>';
			} else {
				info_html += "<li>";
				info_html += "<strong>"+loc_data.extra_data[j].name + ":</strong> "+loc_data.extra_data[j].values[0];
				info_html += "</li>";
			}
			
			
		}
		info_html += '</ul>';
	}
	info_html += '</div>';
	
	makeMarker({
		position: latlng,
		title: loc_data.title,
		map: map,
		content: info_html,
		icon: "${staticbase}icons/google_maps_icon_omakaupunki.png"
	});
	
}
