/*
 * Copyright (C) 2014 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.trustagent.ws.v2;

import com.intel.dcsg.cpg.io.UUID;
import com.intel.mountwilson.common.TAException;
import com.intel.mountwilson.trustagent.commands.hostinfo.HostInfoCmd;
import com.intel.mountwilson.trustagent.data.TADataContext;
import com.intel.mtwilson.launcher.ws.ext.V2;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import com.intel.mtwilson.trustagent.model.HostInfo;
import com.intel.mtwilson.trustagent.model.VMAttestationRequest;
import com.intel.mtwilson.trustagent.model.VMAttestationResponse;
import com.intel.mtwilson.trustagent.vrtmclient.TCBuffer;
import com.intel.mtwilson.trustagent.vrtmclient.Factory;
import com.intel.mtwilson.trustagent.vrtmclient.RPCCall;
import com.intel.mtwilson.trustagent.vrtmclient.RPClient;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.core.Context;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;


/**
 * 
 * XML input “<vm_challenge_request_json><vm_instance_id></ vm_instance_id><vm_challenge_request_json>”
 * JSON input {"vm_challenge_request": {“vm_instance_id":“dcc4a894-869b-479a-a24a-659eef7a54bd"}}
 * JSON output: {“vm_trust_response":{“host_name”:”10.1.1.1”,“vm_instance_id":“dcc4a894-869b-479a-a24a-659eef7a54bd","trust_status":true}}
 * 
 * @author hxia
 */
@V2
@Path("/vrtm")
public class Vrtm {
    
    @POST
    @Path("/reports")
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    public VMAttestationResponse getVMAttesationReport(VMAttestationRequest vmAttestationRequest) throws TAException, IOException {
        
        String vmInstanceId = vmAttestationRequest.getVmInstanceId();
        
        VMAttestationResponse vmAttestationResponse = new VMAttestationResponse();
        
        //read vrtm
        String xmlRPCBlob=  "<?xml version='1.0'?>" 
                            + "<methodCall>"
                            + "<methodName>get_verification_status</methodName>"
                            + 	"<params>"
                            +		"<param>"
                            +			"<value><string>%s</string></value>"
                            +		"</param>"
                            +	"</params>"
                            + "</methodCall>";

        TCBuffer tcBuffer = Factory.newTCBuffer(100, RPCCall.IS_VM_VERIFIED);
		
        // first replace the %s of xmlRPCBlob by VMUUID, rpcore accept all method input arguments in base64 format
        String base64InputArgument = String.format(xmlRPCBlob, DatatypeConverter.printBase64Binary(("vmuuid").getBytes()));
        System.out.println(base64InputArgument);
        tcBuffer.setRPCPayload(base64InputArgument.getBytes());
	
        System.out.println("send");

        // create instance of RPClient, One instance of RPClient for App life time is sufficient 
        // to do processing 
        TCBuffer expResult = null;
        RPClient instance = new RPClient("127.0.0.1", 16005);
        // send tcBuffer to rpcore 
        TCBuffer result = instance.send(tcBuffer);
        
        // process response
        System.out.println("rpid = " + result.getRpId());
        System.out.println("RPC Call Index =" + result.getRPCCallIndex());
        System.out.println("RPC Payload Size = " + result.getRPCPayloadSize());
        System.out.println("RPC Call Status = " + result.getRPCCallStatus());
        System.out.println("RPC Original RP ID = " + result.getOriginalRpId());
        System.out.println("RPC Payload = " + result.getRPCPayload());
        
        /*
        Sample Output:
         <?xml version='1.0'?>
            <methodResponse>
                <params>
                    <param>
                        <value><string>MTAwMA==</string></value>
                    </param>
                </params>
            </methodResponse>
			
	decode MTAwMA== to get actual RP id 	
        */
        
        // close RPClient at the end of application
        instance.close();
        
        //set report
        vmAttestationResponse.setVmInstanceId(vmInstanceId);
        vmAttestationResponse.setTrustStatus(true);
        
        return vmAttestationResponse;

    }
}
