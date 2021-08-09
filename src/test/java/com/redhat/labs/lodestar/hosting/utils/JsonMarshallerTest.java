package com.redhat.labs.lodestar.hosting.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.redhat.labs.lodestar.hosting.exception.HostingException;
import com.redhat.labs.lodestar.hosting.model.HostingEnvironment;

class JsonMarshallerTest {
    
    @Test
    void testObjectMapping() throws Exception{      
        JsonMarshaller json = new JsonMarshaller();        
        
        String test = " [ {\n" + 
                "  \"uuid\" : \"uuid\",\n" + 
                "  \"created\" : \"2021-07-21T17:51:58.774251\",\n" + 
                "  \"updated\" : \"2021-07-21T17:51:58.774251\",\n" + 
                "  \"engagement_uuid\" : \"uuiidde\",\n" + 
                "  \"project_id\" : 1,\n" + 
                "  \"environment_name\" : \"ETL OSP\",\n" + 
                "  \"additional_details\" : \"Will probably both containers and VMs.\",\n" + 
                "  \"ocp_cloud_provider_name\" : \"other\",\n" + 
                "  \"ocp_cloud_provider_region\" : \"other\",\n" + 
                "  \"ocp_persistent_storage_size\" : \"50G\",\n" + 
                "  \"ocp_sub_domain\" : \"cust-1\",\n" + 
                "  \"ocp_version\" : \"4.7.16\",\n" + 
                "  \"ocp_cluster_size\" : \"smallish\"\n" + 
                "}, {\n" + 
                "  \"uuid\" : \"uuid2\",\n" + 
                "  \"created\" : \"2021-07-21T17:51:58.774251\",\n" + 
                "  \"updated\" : \"2021-07-21T17:51:58.774251\",\n" + 
                "  \"engagement_uuid\" : \"uuiidde\",\n" + 
                "  \"project_id\" : 1,\n" + 
                "  \"environment_name\" : \"ETL OSP\",\n" + 
                "  \"additional_details\" : \"Will probably also containers and VMs.\",\n" + 
                "  \"ocp_cloud_provider_name\" : \"other\",\n" + 
                "  \"ocp_cloud_provider_region\" : \"other\",\n" + 
                "  \"ocp_persistent_storage_size\" : \"50G\",\n" + 
                "  \"ocp_sub_domain\" : \"cust-1\",\n" + 
                "  \"ocp_version\" : \"4.7.16\",\n" + 
                "  \"ocp_cluster_size\" : \"smallish\"\n" + 
                "} ]";
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
        LocalDateTime expected = LocalDateTime.parse("2021-07-21T17:51:58.774251", formatter);
        
        
        List<HostingEnvironment> envs = json.fromJson(test);
        assertEquals(2, envs.size());
        assertEquals(expected, envs.get(0).getCreated());
        
    }
    
    @Test
    void testFormatting() {
        String obj = "{}";
        
        JsonMarshaller json = new JsonMarshaller();
        
        assertThrows(HostingException.class, () -> {
            json.fromJson(obj);
        });
        
    }
    
}
