package com.revature.controllers;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class Helper {

	@Autowired
	RestTemplate restTemplate;
	
	@Autowired
	ObjectMapper objectMapper;

	/* Returns id assigned to slack channel with given name
	 * Use for methods that require slack channel id (e.g. editing channel name)
	 */
	public String getChannelId(String name, String userToken) {
	
		JsonNode channelList = getChannelList(userToken);
		Iterator<JsonNode> channels = channelList.elements();
			
		while(channels.hasNext()) {
			JsonNode nextChannel = channels.next();
			JsonNode nextChannelName = nextChannel.path("name");
			if(name.equals(nextChannelName.asText())) {
				return nextChannel.path("id").asText();
			}
		}
		
		return null;
	}
	
	public boolean isAdmin(String token, String email) {
		String id = getSlackId(token, email);
		System.out.println("id :"+id);
		String requestUrl = "https://slack.com/api/users.info";
		String is_admin = "";
		
		MultiValueMap<String, String> params = 
				new LinkedMultiValueMap<String, String>();
		params.add("token", token);
		params.add("user", id);
			
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType( MediaType.APPLICATION_FORM_URLENCODED);
		
		HttpEntity<MultiValueMap<String, String>> request = 
				new HttpEntity<MultiValueMap<String, String>>( params, headers);
		
		String responseString = restTemplate.postForObject( requestUrl, request, String.class);
		
		JsonNode rootNode, userNode, adminNode = null;
		
			try {
				rootNode = objectMapper.readTree( responseString);
			userNode = rootNode.path( "user");
			adminNode = userNode.path( "is_admin");
			is_admin = adminNode.asText();
			if(is_admin.equals("true"))
				return true;
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return false;
	}
	
	public String getToken() {
/*		Properties prop = new Properties();
		InputStream in;
		try {
			in = new FileInputStream("slack.properties");

			prop.load(in);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return prop.getProperty("client_token");*/
		//should be in a file at the very least, realy should be handled through spring security
		return "xoxp-237895291120-239988622849-247757395381-6621e1f74bb7e699c842576882b877f2";
	}
	
	//get a user's slack id from their email
	public String getSlackId(String token, String email) {
		JsonNode usersList = getUserList(token);
		Iterator<JsonNode> users = usersList.elements();
			System.out.println("user list: " +usersList);
		while(users.hasNext()) {
			JsonNode nextUser = users.next();
			//System.out.println(users.next().asText());
			JsonNode nextUserEmail = nextUser.path("profile").path("email");
			//System.out.println(nextUser.path("profile").path("email").asText());
			if(email.equals(nextUserEmail.asText())) {
				return nextUser.path("id").asText();
			}
		}
		
		return null;
	}
	
	//retrieve all users in team
	public JsonNode getUserList(String userToken) {
		
		String url = "https://slack.com/api/users.list";
		
		MultiValueMap<String, String> params = 
				new LinkedMultiValueMap<String, String>();
		params.add("token", userToken);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		HttpEntity<MultiValueMap<String, String>> request = 
				new HttpEntity<MultiValueMap<String, String>>(params, headers);
		
		String responseString = 
				restTemplate.postForObject(url, request, String.class);
		//String responseString = restTemplate.getForObject(url, String.class);
		JsonNode root, channelElement = null;
		Iterator<JsonNode> channels = null;
		
		try {
			root = objectMapper.readTree(responseString);
			channelElement = root.path("members");
			return channelElement;
		}catch(IOException e) {
			e.printStackTrace();
		}
		
		return null;
		
	}
	
	
	public String scopes(String token) {
		StringBuilder scopes = new StringBuilder("");
		try {
	    	URL obj = new URL("https://slack.com/api/users.identity?token=" + token);
	    	URLConnection conn = obj.openConnection();
	    	
	    	
	    	Map<String, List<String>> map = conn.getHeaderFields();
	    	
	    	for (Map.Entry<String, List<String>> entry : map.entrySet()) {
/*	    		System.out.println("Key : " + entry.getKey()
	                               + " ,Value : " + entry.getValue());*/
	    		//check for client scope
	    		for(int i = 0; i < entry.getValue().size(); ++i) {
	    		if(entry.getValue().get(i).contains("client"))
	    			scopes.append("client");
	    		if(entry.getValue().get(i).contains("channels:write"))
	    			scopes.append("channels:write");
	    		}
	    	}
		    System.out.println(scopes.toString());
	    	}catch (Exception e) {
	    		e.printStackTrace();
	        }
	    return scopes.toString();
	}
	
	public List<String> getAllChannels(String token){
		List<String>chanList = new ArrayList<String>();
		
		JsonNode channelList = getChannelList(token);
		Iterator<JsonNode> channels = channelList.elements();
		
		while(channels.hasNext()) {
			String name = channels.next().path("name").asText();
			String id = channels.next().path("id").asText();
			
			chanList.add("{ \"name\":  \"" + name + "\" , \"id\": \""+ id + "\" }" );
		}
		
		return chanList;
	}
	
	public boolean channelNameIsUnique(String name, String userToken) {
		JsonNode channelList = getChannelList(userToken);
		Iterator<JsonNode> channels = channelList.elements();
		
		while(channels.hasNext()) {
			String currentName = channels.next().path("name").asText();
			if(name.equals(currentName)){return false;}
		}
		
		return true;
	}
	
	public JsonNode getChannelList(String userToken) {
		
		String url = "https://slack.com/api/channels.list";
		
		MultiValueMap<String, String> params = 
				new LinkedMultiValueMap<String, String>();
		params.add("token", userToken);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		HttpEntity<MultiValueMap<String, String>> request = 
				new HttpEntity<MultiValueMap<String, String>>(params, headers);
		
		String responseString = 
				restTemplate.postForObject(url, request, String.class);
	
		//String responseString = restTemplate.getForObject(url, String.class);
		JsonNode root, channelElement = null;
		Iterator<JsonNode> channels = null;
		
		try {
			root = objectMapper.readTree(responseString);
			channelElement = root.path("channels");
			return channelElement;
		}catch(IOException e) {
			e.printStackTrace();
		}
		
		return null;
		
	}
	
	/*	Returns ArrayList with the slack ID's of all users in a given channel*/
	
	public List<String> getAllUsersInChannel(String channelName, String userToken) {
		
		String channelId = getChannelId(channelName, userToken);
		
		String requestUrl = "https://slack.com/api/channels.info";
		
		MultiValueMap<String, String> params = 
				new LinkedMultiValueMap<String, String>();
		params.add("token", userToken);
		params.add("channel", channelId);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		HttpEntity<MultiValueMap<String, String>> request = 
				new HttpEntity<MultiValueMap<String, String>>(params, headers);
		
		String responseString = restTemplate.postForObject(requestUrl, request, String.class);
		JsonNode rootNode, channelNode, memNode = null;
		List<String> users = new ArrayList<String>();
		//System.out.println(responseString);
		
		try {
			rootNode = objectMapper.readTree(responseString);
			channelNode = rootNode.path("channel");
			memNode = channelNode.path("members");
			
			Iterator<JsonNode> elements = memNode.elements();
			while(elements.hasNext()){
				JsonNode member = elements.next();
				users.add("{ \"name\":  \"" + getUserName(member.asText(), userToken) + "\" , \"id\": \""+member.asText() + "\" }" );
			}
			//System.out.println(users);
			return users;
		}catch(IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	/*	Returns string with UserName given slack users ID*/
	public String getUserName(String userId, String userToken) {
		
		String requestUrl = "https://slack.com/api/users.info";
		
		MultiValueMap<String, String> params = 
				new LinkedMultiValueMap<String, String>();
		params.add("token", userToken);
		params.add("user", userId);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		HttpEntity<MultiValueMap<String, String>> request = 
				new HttpEntity<MultiValueMap<String, String>>(params, headers);
		
		String responseString = restTemplate.postForObject(requestUrl, request, String.class);
		JsonNode rootNode, userNode, idNode = null;
		String userName = "";
		
		try {
			rootNode = objectMapper.readTree(responseString);
			userNode = rootNode.path("user");
			idNode = userNode.path("name");
			userName = idNode.asText();
			
			return userName;
		}catch(IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public String  startConvo(List<String> userIds, String channelId, String userToken) {
		
		String tag = "channel";
		String requestUrl = "https://slack.com/api/conversations.open?token=" + userToken +
				 "&return_im=false" + "&users=";
		if(userIds.size()>1) {
			requestUrl = "https://slack.com/api/mpim.open?token=" + userToken +"&users=";
			tag = "group";
		}
		
		for(int i = 0; i<(userIds.size()-1); i++) {
			requestUrl += userIds.get(i) + ",";
		}
		requestUrl += userIds.get(userIds.size()-1);
		requestUrl +="&pretty=1";
		
		String responseString = restTemplate.getForObject(requestUrl, String.class);
		JsonNode rootNode, chanNode, idNode = null;
		String channel = "";
		
		try {
			rootNode = objectMapper.readTree(responseString);
			chanNode = rootNode.path(tag);
			idNode = chanNode.path("id");
			channel = idNode.asText();
			

			return channel;
		}catch(IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public String  directMessage(String token, String channelId, String message, List<String> userIds) {
		
		String newChannel = startConvo(userIds, channelId, token);
		
		String requestUrl = "https://slack.com/api/chat.postMessage";
		
		MultiValueMap<String, String> params = 
				new LinkedMultiValueMap<String, String>();
		params.add("token", token);
		params.add("channel", newChannel);
		params.add("return_im", "false");
		params.add("text", message);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		HttpEntity<MultiValueMap<String, String>> request = 
				new HttpEntity<MultiValueMap<String, String>>(params, headers);
	 
		String response = restTemplate.postForObject(requestUrl, request, String.class);
			
		return response;
	}
	
	public String  multiMessage(String token, String channelId, String message, List<String> userIds) {
		
		List<String> channels = new ArrayList<String>();
		String responseString = "";
		
		for(int i = 0; i < userIds.size(); i++) {
			channels.add(startConvo(userIds, channelId, token) );
			List<String> singleUser = new ArrayList<String>();
			singleUser.add(userIds.get(i));
			
			responseString = directMessage(token, channelId, message, singleUser);
		}
				
		return responseString;
	}
	
	//Extracts name parameter from json  
	public String nameParameter(String json) {
		String channelName = json.split(":")[1];
		channelName = channelName.substring(1, channelName.length()-2);
		return channelName;
	}
	
    //Extracts user ids from json
    public List<String> getIdList(String ids){
        
        JsonNode rootNode = null;
        List<String> users = new ArrayList<String>();
        
        try {
            rootNode = objectMapper.readTree(ids);
            
            Iterator<JsonNode> elements = rootNode.elements();
            while(elements.hasNext()){
                JsonNode member = elements.next();
                users.add(member.findValue("id").asText());
                
            }
           // System.out.println(users);
            return users;
        }catch(IOException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    public String truncate(String complexName) {
    	String shortenedComplexName;
    	if(complexName.length() > 10) {
			shortenedComplexName =complexName.replaceAll("\\s","").substring(0, 10).toLowerCase();
		} else {
			shortenedComplexName = complexName.replaceAll("\\s","").toLowerCase();
		}
    	
    	return shortenedComplexName;
    }
    
    public String complexChannelName(String complex) {
    	return truncate(complex);
    }
    
    public String unitChannelName(String complex, String building, String unit) {
    	return truncate(complex) + building + "-" + unit;
    }
	
}
