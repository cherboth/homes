angular.module('rhmsApp').controller('loginController', ['$scope', '$http', '$rootScope', '$location','$window', function($scope, $http, $rootScope, $location,$window){
/*    $http.get("/HousingOnlineManagementSystem/api/login").then(function(response) {
        $scope.code = response.data;
    });*/
	
    if(!$rootScope.rootUser == undefined){
    	$state.go("home.dashboard");
    }
    
    var paramValue = $location.search().code;
	$http.post("/api/slack/manager/scopes/basic",{code : paramValue}).then(function(response) {
		
		//$scope.user = resmponse.data;
		//$location.path("/home/dashboard");
		if(response.data.scope === "basic"){
	        	$window.location.href = "https://slack.com/oauth/authorize?scope=channels:write,channels:read,chat:write,users:read,users:read.email,groups:write,im:write,mpim:write&client_id=237895291120.238685216005";	
    	}else if(response.data.scope === "client"){
				$window.location.href = "https://slack.com/oauth/authorize?scope=client&client_id=237895291120.238685216005";
    	}  else if(response.data.scope === ""){
    	}else{
    		$rootScope.rootUser = response.data;
    		$location.path("/home/dashboard");
    	}
    });

}]);
