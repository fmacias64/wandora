/*
 * WANDORA
 * Knowledge Extraction, Management, and Publishing Application
 * http://wandora.org
 * 
 * Copyright (C) 2004-2016 Wandora Team
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * 
 * CopyAssociations.java
 *
 * Created on 6. tammikuuta 2005, 16:23
 */

package org.wandora.application.tools.associations;



import org.wandora.application.tools.*;
import org.wandora.topicmap.*;
import org.wandora.application.*;
import org.wandora.application.contexts.*;
import org.wandora.utils.*;
import java.util.*;
import org.wandora.application.gui.topicstringify.TopicToString;


/**
 * <p>
 * Tool is used to generate textual representations of given associations. Tool
 * puts the generated association representation to system clipboard.
 * </p>
 * <p>
 * Tool is capable to generate plain text and HTML table representation. Default
 * format is plain text.
 * </p>
 * 
 * @author  akivela
 */
public class CopyAssociations extends AbstractWandoraTool implements WandoraTool {
    
    public static final int WANDORA_LAYOUT = 5000;
    public static final int LTM_LAYOUT = 5010;
    
    public static final int TABTEXT_OUTPUT = 2000;
    public static final int HTML_OUTPUT = 2010;
    
    
    private int outputFormat = 0;
    private int layout = WANDORA_LAYOUT;

    
    public CopyAssociations(Wandora admin, Context context)  throws TopicMapException {
        this(admin, context, TABTEXT_OUTPUT);
    }
    
    public CopyAssociations(Wandora admin, Context context, int outputFormat)  throws TopicMapException {
        this.outputFormat = outputFormat;
        setContext(context);
    }
    
    public CopyAssociations(Wandora admin, Context context, int outputFormat, int outputLayout)  throws TopicMapException {
        this.outputFormat = outputFormat;
        this.layout = outputLayout;
        setContext(context);
    }
    
    
    
    public CopyAssociations(Context context) {
        this(context, TABTEXT_OUTPUT);
    }
    
    
    public CopyAssociations(Context context, int outputFormat) {
        setContext(context);
        this.outputFormat = outputFormat;
    }
    
    
    public CopyAssociations(Context context, int outputFormat, int outputLayout) {
        setContext(context);
        this.outputFormat = outputFormat;
        this.layout = outputLayout;
    }
    
    
    public CopyAssociations(int outputFormat) {
        this(new AssociationContext(), outputFormat);
    }
    
    
    public CopyAssociations(int outputFormat, int outputLayout) {
        this(new AssociationContext(), outputFormat, outputLayout);
    }
    
    
    
    public CopyAssociations() {
        this(new AssociationContext(), TABTEXT_OUTPUT);
    }
    
    
    // -------------------------------------------------------------------------
    

    public void setOutputFormat(int outputFormat) {
        this.outputFormat = outputFormat;
    }
    
    
    // -------------------------------------------------------------------------
    
    
    @Override
    public void execute(Wandora admin, Context context)  throws TopicMapException {
        String associationText = makeString(admin);
        if(associationText != null && associationText.length() > 0) {
            ClipboardBox.setClipboard(associationText);
        }
    }
    
    
    
    
    public String makeString(Wandora admin)  throws TopicMapException {
        StringBuilder sb = new StringBuilder("");

        HashMap hash = new HashMap();
        HashMap<Topic,ArrayList<HashMap<Topic,Topic>>> associationsByType = new HashMap();
        HashMap<Topic,HashSet<Topic>> rolesByType = new HashMap();

        Iterator associations = null;
        Iterator context = getContext().getContextObjects();
        Association a = null;
        Topic t = null;
        Object aort = null;
        int count = 0;

        if(context != null) {
            setDefaultLogger();
            log("Copying associations...");
            while(context.hasNext()) {
                aort = context.next();
                associations = null;
                if(aort instanceof Topic) {
                    t = (Topic) aort;
                    associations = t.getAssociations().iterator();
                }
                else if(aort instanceof Association) {
                    a = (Association) aort;
                    ArrayList as = new ArrayList();
                    as.add(a);
                    associations = as.iterator();
                }
                if(associations == null) continue;
                
                // Ok, at this point we should have a valid association iterator.
                while(associations.hasNext()) {
                    count++;
                    setProgress(count);
                    a = (Association) associations.next();
                    Topic type = a.getType();
                    hlog("Copying association of type '" + getNameFor(type) + "'.");
                    ArrayList typedAssociations = associationsByType.get(type);
                    if(typedAssociations == null) typedAssociations = new ArrayList();

                    Collection aRoles = a.getRoles();
                    HashSet roles = rolesByType.get(type);
                    if(roles == null) roles = new LinkedHashSet();
                    HashMap association = new LinkedHashMap();

                    for(Iterator aRoleIter = aRoles.iterator(); aRoleIter.hasNext(); ) {
                        Topic role = (Topic) aRoleIter.next();
                        Topic player = a.getPlayer(role);
                        association.put(role, player);
                        roles.add(role);
                    }
                    typedAssociations.add(association);
                    rolesByType.put(type, roles);
                    associationsByType.put(type, typedAssociations);
                }
            }

            if(count != 0) {
                log("Formatting output...");
                boolean HTMLOutput = (outputFormat == HTML_OUTPUT);
                // ----- Transform created data structure to text&html! -----
                
                if(layout == WANDORA_LAYOUT) {
                    for(Topic associationType : associationsByType.keySet()) {
                        sb.append(getNameFor(associationType)).append("\n");
                        if(HTMLOutput) sb.append("<br>\n<table>\n<tr>");
                        for(Topic role : rolesByType.get(associationType)) {
                            if(HTMLOutput) sb.append("<td>");
                            sb.append(getNameFor(role));
                            if(HTMLOutput) sb.append("</td>");
                            else sb.append("\t");
                        }
                        sb.deleteCharAt(sb.length()-1); // remove last tabulator
                        
                        if(HTMLOutput) sb.append("</tr>");
                        sb.append("\n");
                        for(HashMap<Topic,Topic> association : associationsByType.get(associationType)) {
                            if(HTMLOutput) sb.append("<tr>");
                            for(Topic role : rolesByType.get(associationType)) {
                                if(HTMLOutput) sb.append("<td>");
                                Topic player = association.get(role);
                                sb.append( getNameFor(player) );
                                if(HTMLOutput) sb.append("</td>");
                                else sb.append("\t");
                            }
                            sb.deleteCharAt(sb.length()-1); // remove last tabulator
                            if(HTMLOutput) sb.append("</tr>");
                            sb.append("\n");
                        }
                        if(HTMLOutput) sb.append("</table>");
                        sb.append("\n");
                    }
                }
                
                else if(layout == LTM_LAYOUT) {
                    for(Topic associationType : associationsByType.keySet()) {
                        if(HTMLOutput) sb.append("<br>\n<table>\n");
                        for(HashMap<Topic,Topic> association : associationsByType.get(associationType)) {
                            if(HTMLOutput) sb.append("<tr>");
                            if(HTMLOutput) sb.append("<td>");
                            sb.append(getNameFor(associationType));
                            if(HTMLOutput) sb.append("</td>");
                            else sb.append("\t");
                            for(Topic role : rolesByType.get(associationType)) {
                                Topic player = association.get(role);
                                if(player != null) {
                                    if(HTMLOutput) sb.append("<td>");
                                    sb.append( getNameFor(player) );
                                    if(HTMLOutput) sb.append("</td>");
                                    else sb.append("\t");
                                    
                                    if(HTMLOutput) sb.append("<td>");
                                    sb.append( getNameFor(role) );
                                    if(HTMLOutput) sb.append("</td>");
                                    else sb.append("\t");
                                }
                            }
                            sb.deleteCharAt(sb.length()-1); // remove last tabulator
                            if(HTMLOutput) sb.append("</tr>");
                            sb.append("\n");
                        }
                        if(HTMLOutput) sb.append("</table>");
                        sb.append("\n");
                    }
                }
                log("Total "+count+" associations copied.");
                log("Total "+associationsByType.size()+" different association types found.");
            }
            else {
                log("No associations found.");
            }
            log("Ready.");
            setState(WAIT);
        }
        return(sb.toString());
    }

    
    

    
    
    public String getNameFor(Topic t)  throws TopicMapException {
        if(t != null) {
            return TopicToString.toString(t);
        }
        return "";
    }
    
    
    @Override
    public String getName() {
        return "Copy associations";
    }

    @Override
    public String getDescription() {
        return "Copy selected associations to clipboard.";
    }
    
    @Override
    public boolean requiresRefresh() {
        return false;
    }
    
}
