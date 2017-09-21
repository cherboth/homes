package com.revature.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.websocket.server.PathParam;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordnik.swagger.annotations.Api;

@Api(value="unit", description="This service allows users to message other users or their channel over slack")
@RestController
@RequestMapping("resident")
public class ResidentController {
	
	@Autowired
	RestTemplate restTemplate;
	
	@Autowired
	Helper helper;
	
	@Autowired
	ObjectMapper objectMapper;
	
	@Autowired
	ObjectMapper objectMapper;
	
	/*Sends an email invite to the email and autofills first name and last name for slack registration
	does not enforce names*/
	@PostMapping("invite")
	public  ResponseEntity<String> invite(@RequestBody String body, HttpSession http){
		
		JSONObject json = null;
		String email = null; String firstName = null; String lastName = null;
		try {
			json = new JSONObject(body);
			email = json.getString("email");
			firstName = json.getString("fname");
			lastName = json.getString("lname");
		}catch(JSONException e) {
			e.printStackTrace();
		}
		
		SecurityContext sc = (SecurityContextImpl) http.getAttribute("SPRING_SECURITY_CONTEXT");
		OAuth2AuthenticationDetails details = (OAuth2AuthenticationDetails) sc.getAuthentication().getDetails();
		String token =  details.getTokenValue();
	
		String requestUrl = "https://slack.com/api/users.admin.invite";
	
		MultiValueMap<String, String> params = 
			new LinkedMultiValueMap<String, String>();
		params.add("token", token);
		params.add("email", email);
		params.add("first_name", firstName);
		params.add("last_name", lastName);
	
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
	
		HttpEntity<MultiValueMap<String, String>> request = 
			new HttpEntity<MultiValueMap<String, String>>(params, headers);

		ResponseEntity<String> response = restTemplate.postForEntity(requestUrl, request, String.class);
	
		return response;
	}

	@PostMapping("message")
	public ResponseEntity<String> messageChannel(@RequestBody String body, HttpSession http){
		
		JSONObject json = null;
		String complex = null; String unit = null; String message = null;
		try {
			json = new JSONObject(body);
			complex = json.getString("complex");
			unit = json.getString("unit");
			message = json.getString("message");
		}catch(JSONException e) {
			e.printStackTrace();
		}
		
		SecurityContext sc = (SecurityContextImpl) http.getAttribute("SPRING_SECURITY_CONTEXT");
		OAuth2AuthenticationDetails details = (OAuth2AuthenticationDetails) sc.getAuthentication().getDetails();
		String token =  details.getTokenValue();
		
		String channelName = complex + unit;
		String channelId = helper.getChannelId(channelName, token);
		
		String requestUrl  = "https://slack.com/api/chat.postMessage";
		
		MultiValueMap<String, String> params = 
				new LinkedMultiValueMap<String, String>();
		params.add("token", token);
		params.add("channel", channelId);
		params.add("text", message);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		HttpEntity<MultiValueMap<String, String>> request = 
				new HttpEntity<MultiValueMap<String, String>>(params, headers);
	 
		ResponseEntity<String> response = restTemplate.postForEntity(requestUrl, request, String.class);
		
		return response;
		
	}
	
	/*sends a message and @mentions all users in the userIds List*/
	@PostMapping("message/users")
	public ResponseEntity<Object> messageUser(@RequestBody String body, HttpSession http){
		
		JSONObject json = null;
		String complex = null; String unit = null; String message = null; 
		String ids = null;
		try {
			json = new JSONObject(body);
			complex = json.getString("complex");
			unit = json.getString("unit");
			message = json.getString("message");
			ids = json.getString("ids");
		}catch(JSONException e) {
			e.printStackTrace();
		}
		
		SecurityContext sc = (SecurityContextImpl) http.getAttribute("SPRING_SECURITY_CONTEXT");
		OAuth2AuthenticationDetails details = (OAuth2AuthenticationDetails) sc.getAuthentication().getDetails();
		String token =  details.getTokenValue();
		
		List<String>userIds = new ArrayList<String>(Arrays.asList(ids.split(",")));
		String channelName = complex + unit;
		String channelId = helper.getChannelId(channelName, token);
		
		String requestUrl  = "https://slack.com/api/chat.postMessage?token=" + token +
				 "&channel="+ channelId+"&text=" + message;
		for(int i = 0; i<userIds.size(); i++) {
			requestUrl += " <@" + userIds.get(i) + ">";
		}
		
		String responseString = restTemplate.getForObject(requestUrl, String.class);
		return ResponseEntity.ok(responseString);
		
	}
	
	@PostMapping("message/users/direct")
	public ResponseEntity<Object> messageUserDirect(@RequestBody String body, 
			HttpServletRequest req, HttpSession http){
		
		JSONObject json = null;
		String complex = null; String unit = null; String message = null; 
		String group = null; String ids = null;
		try {
			json = new JSONObject(body);
			complex = json.getString("complex");
			unit = json.getString("unit");
			message = json.getString("message");
			group = json.getString("group");
			ids = json.getString("ids");
		}catch(JSONException e) {
			e.printStackTrace();
		}
		
		SecurityContext sc = (SecurityContextImpl) http.getAttribute("SPRING_SECURITY_CONTEXT");
		OAuth2AuthenticationDetails details = (OAuth2AuthenticationDetails) sc.getAuthentication().getDetails();
		String token =  details.getTokenValue();
		
		List<String>userIds = new ArrayList<String>(Arrays.asList(ids.split(",")));
		System.out.println(userIds);
		
		String channelName = complex + unit;
		String channelId = helper.getChannelId(channelName, token);
		String responseString ="";
		
		//individual message/s
		if(group.equals("0")) {
			responseString = helper.multiMessage(token, channelId, message, userIds);
		}
		//group direct message
		else {
			responseString = helper.directMessage(token, channelId, message, userIds);
		}

		return ResponseEntity.ok(responseString);
		
	}
	
	/*kicks a user from an apartment channel*/
	@PostMapping("kick/users")
	public ResponseEntity<String> kickUser(@RequestBody String body,
			HttpSession http){
		
		JSONObject json = null;
		String complex = null; String unit = null; String message = null; 
		String userId = null;
		try {
			json = new JSONObject(body);
			complex = json.getString("complex");
			unit = json.getString("unit");
			message = json.getString("message");
			userId = json.getString("userId");
		}catch(JSONException e) {
			e.printStackTrace();
		}
		
		SecurityContext sc = (SecurityContextImpl) http.getAttribute("SPRING_SECURITY_CONTEXT");
		OAuth2AuthenticationDetails details = (OAuth2AuthenticationDetails) sc.getAuthentication().getDetails();
		String token =  details.getTokenValue();
		
		String channelName = complex + unit;
		String channelId = helper.getChannelId(channelName, token);
		
		String requestUrl  = "https://slack.com/api/channels.kick";
		
		MultiValueMap<String, String> params = 
				new LinkedMultiValueMap<String, String>();
		params.add("token", token);
		params.add("channel", channelId);
		params.add("user", userId);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		HttpEntity<MultiValueMap<String, String>> request = 
				new HttpEntity<MultiValueMap<String, String>>(params, headers);
		
		ResponseEntity<String> response = restTemplate.postForEntity(requestUrl, request, String.class);
		return response;
		
	}
	
	/*send list of [userid1,username1, uid2,un2,...] to front end*/
	@RequestMapping(value="message/listusers", method=RequestMethod.GET)
	public ResponseEntity<Object> listUser(@RequestBody String body, HttpSession http){
		
		JSONObject json = null;
		String complex = null; String unit = null;
		try {
			json = new JSONObject(body);
			complex = json.getString("complex");
			unit = json.getString("unit");
		}catch(JSONException e) {
			e.printStackTrace();
		}
		
		SecurityContext sc = (SecurityContextImpl) http.getAttribute("SPRING_SECURITY_CONTEXT");
		OAuth2AuthenticationDetails details = (OAuth2AuthenticationDetails) sc.getAuthentication().getDetails();
		String token =  details.getTokenValue();
		
		String channelName = complex + unit;
		List<String> userList = helper.getAllUsersInChannel(channelName, token);
		//System.out.println(userList);
		
		return ResponseEntity.ok(userList);
		
	}
	
<<<<<<< HEAD
=======
	/*Add a user to the slack channel for their complex and unit*/
	@PostMapping("complexInvite")
	public ResponseEntity<String> complexInvite(@RequestBody String body, HttpSession http){
		
		JSONObject json = null;
		String complex = null;
		String userId = null;
		String email = null;
		try {
			json = new JSONObject(body);
			complex = json.getString("complex");
			email = json.getString("email");
		}catch(JSONException e) {
			e.printStackTrace();
		}
		System.out.println(complex);
		
		SecurityContext sc = (SecurityContextImpl) http.getAttribute("SPRING_SECURITY_CONTEXT");
		OAuth2AuthenticationDetails details = (OAuth2AuthenticationDetails) sc.getAuthentication().getDetails();
		String token =  details.getTokenValue();
		userId = helper.getSlackId(token, email);
		String requestUrl = "https://slack.com/api/channels.invite";
		
		String complexChannelId = helper.getChannelId(complex, token);
		
		/*Request Slack to invite user to complex channel*/
		
		MultiValueMap<String, String> complexParams = 
				new LinkedMultiValueMap<String, String>();
		complexParams.add("token", token);
		complexParams.add("channel", complexChannelId);
		complexParams.add("user", userId);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		HttpEntity<MultiValueMap<String, String>> request = 
				new HttpEntity<MultiValueMap<String, String>>(complexParams, headers);
		
		ResponseEntity<String> response = restTemplate.postForEntity(requestUrl, request, String.class);
		return response;
	}
	
	/*Add a user to the slack channel for their complex and unit*/
	@PostMapping("unitInvite")
	public ResponseEntity<String> unitInvite(@RequestBody String body, HttpSession http){
		
		JSONObject json = null;
		String complex = null; String unit = null;
		String userId = null;
		try {
			json = new JSONObject(body);
			complex = json.getString("complex");
			unit = json.getString("unit");
			userId = json.getString("userId");
		}catch(JSONException e) {
			e.printStackTrace();
		}
		
		SecurityContext sc = (SecurityContextImpl) http.getAttribute("SPRING_SECURITY_CONTEXT");
		OAuth2AuthenticationDetails details = (OAuth2AuthenticationDetails) sc.getAuthentication().getDetails();
		String token =  details.getTokenValue();
		
		String requestUrl = "https://slack.com/api/channels.invite";
		
		String unitChannelId = helper.getChannelId(complex + unit, token);
		
		/*Request Slack to invite user to complex channel*/
		
		MultiValueMap<String, String> complexParams = 
				new LinkedMultiValueMap<String, String>();
		complexParams.add("token", token);
		complexParams.add("channel", unitChannelId);
		complexParams.add("user", userId);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		HttpEntity<MultiValueMap<String, String>> request = 
				new HttpEntity<MultiValueMap<String, String>>(complexParams, headers);
		
		ResponseEntity<String> response = restTemplate.postForEntity(requestUrl, request, String.class);
		return response;
	}
>>>>>>> Slack-Docs
	
	@PostMapping("admin")
	public ResponseEntity<String> isAdmin(@RequestBody String body, HttpSession http){
		
		JSONObject json = null;
		String userId = null;
		String isAdmin = null;
		try {
			json = new JSONObject(body);
			userId = json.getString("userId");
			isAdmin = json.getString("isAdmin");
			
		} catch(JSONException e) {
			e.printStackTrace();
		}
		
		SecurityContext sc = (SecurityContextImpl) http.getAttribute("SPRING_SECURITY_CONTEXT");
		OAuth2AuthenticationDetails details = (OAuth2AuthenticationDetails) sc.getAuthentication().getDetails();
		String token =  details.getTokenValue();
		
		
		String requestUrl = "https://slack.com/api/users.info";
		
		MultiValueMap<String, String> params = 
				new LinkedMultiValueMap<String, String>();
		params.add("token", token);
		params.add("user", userId);
			
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
			isAdmin = adminNode.asText();
			
			ResponseEntity<String> responseEntity =  new ResponseEntity<>("{isAdmin:" + isAdmin + "}", HttpStatus.OK);
			
			return responseEntity;
			
		} catch(IOException e) {
			e.printStackTrace();
		}
		return null;
	}
<<<<<<< HEAD
=======
	
>>>>>>> Slack-Docs
}
