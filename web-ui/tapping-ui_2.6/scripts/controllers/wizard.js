'use strict';

angular.module('controllerUxApp')
  .controller('wizard', function ($scope, $routeParams, $location, backend, modals, types, $rootScope, $timeout) {
	  
	if ($routeParams.id == "new") {
		var newPolicy = true;
		$scope.policy = types.new("tapPolicy");
	}
	else {
		$scope.policy = backend.lookup("tapPolicy", $routeParams.id);
	}
	
	$scope.backend = backend;
	$scope.modals = modals;
	  
    $scope.currentTab = "info";
    $scope.showTab = function(tabName) {
    	return tabName == $scope.currentTab;
    };
    $scope.setTab = function(tab) {
    	$scope.currentTab = tab;
    };
    
    $scope.done = function() {
    	
    	var backendAction = newPolicy ?
    			backend.addTo("tapPolicy", $scope.policy) :
    			backend.update("tapPolicy", $scope.policy)
    			
    	backendAction.then(function(response) {
    		$location.path('/');
    	})
    };
    
    $scope.cancel = function() {
    	$location.path('/');
    	$timeout(function() { $rootScope.$emit("scrollDownLogs"); }, 100);    };
  });
