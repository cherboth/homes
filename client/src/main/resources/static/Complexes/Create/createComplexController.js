angular.module('rhmsApp').controller('createComplexController', ['$scope', '$http', '$mdDialog','$state','$mdToast', function($scope, $http, $mdDialog, $state, $mdToast) {

	$scope.place = {};
	$scope.complex = {};

	$http.get('/api/complex/office')
		.then(function(response){
			$scope.offices = response.data;
		}, function(response){
			$mdToast.show($mdToast.simple().textContent("An Error Occured. Error " + response.status).position('top right'));
		});
	
    $scope.newComplexFormSubmit = function () {
    	
    	$scope.complex.office = {};
    	$scope.complex.office.officeId = JSON.parse($scope.selected).officeId;
       	
        var onSuccess = function (data, status, headers, config) {
            $scope.complex.office = {};
        	$scope.complex.office.id = $scope.selected.id;
        	$mdToast.show($mdToast.simple().textContent("Complex Created").position('top right'));
            $state.go('home.showComplex', { complexId: data});
            
        };

        var onError = function (data, status, headers, config) {
        	 $mdToast.show($mdToast.simple().textContent("An Error Occured").position('top right'));
        }

        $http.post('/api/complex/complex/', $scope.complex )
            .success(onSuccess)
            .error(onError);
    };

    $scope.resetForm = function () {
        $scope.complex = "";
    };
}]);
