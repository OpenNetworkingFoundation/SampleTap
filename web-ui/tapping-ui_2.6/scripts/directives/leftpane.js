'use strict';

angular.module('controllerUxApp')
  .directive('leftpane', function($rootScope) {
    return {
      templateUrl: "views/leftpanetemplate.html",
      restrict: 'E',
      scope: {},
      controller: function($scope) {
                  $scope.backend = $scope.$parent.backend;
                  $scope.modals = $scope.$parent.modals;
                  $scope.currentTab = "tappingObjects";
                  $scope.showTab = function(tabName) {
                  	return tabName == $scope.currentTab;
                  };
                  $scope.setTab = function(tab) {
                  	$scope.currentTab = tab;
                  };
      },
      link: function postLink(scope, element, attrs) {
      }
    };
  });