package com.kyc.snap.api;

import com.fasterxml.jackson.annotation.JsonValue;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/files")
public interface FileService {

    @POST
    @Path("/")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    StringJson uploadFile(byte[] data);

    record StringJson(@JsonValue String value) {}

    @GET
    @Path("/{fileId}")
    @Produces(MediaType.WILDCARD)
    byte[] getFile(@PathParam("fileId") String fileId);
}
