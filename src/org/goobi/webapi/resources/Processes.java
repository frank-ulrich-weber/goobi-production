/*
 * This file is part of the Goobi Application - a Workflow tool for the support of
 * mass digitization.
 *
 * Visit the websites for more information.
 *     - http://gdz.sub.uni-goettingen.de
 *     - http://www.goobi.org
 *     - http://launchpad.net/goobi-production
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place,
 * Suite 330, Boston, MA 02111-1307 USA
 */

package org.goobi.webapi.resources;

import com.sun.jersey.api.NotFoundException;
import org.goobi.webapi.beans.GoobiProcess;
import org.goobi.webapi.beans.GoobiProcessStep;
import org.goobi.webapi.beans.IdentifierPPN;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

@Path("/processes")
public class Processes {

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<GoobiProcess> getProcesses() {
        List<GoobiProcess> processes = new ArrayList<GoobiProcess>();

        processes.addAll(org.goobi.webapi.dao.GoobiProcess.getAllProcesses());

        return processes;
    }

    @GET
    @Path("{ppnIdentifier}")
    public GoobiProcess getProcess(@PathParam("ppnIdentifier") IdentifierPPN ippn) {

        GoobiProcess process = org.goobi.webapi.dao.GoobiProcess.getProcessByPPN(ippn);

        if (process == null) {
            throw new NotFoundException("No such process.");
        }

        return process;
    }

    @GET
    @Path("{ppnIdentifier}/steps")
    public List<GoobiProcessStep> getProcessSteps(@PathParam("ppnIdentifier") IdentifierPPN ippn) {

        List<GoobiProcessStep> resultList = org.goobi.webapi.dao.GoobiProcess.getAllProcessSteps(ippn);

        if (resultList.isEmpty()) {
            throw new NotFoundException("No such process.");
        }

        return resultList;
    }

}
