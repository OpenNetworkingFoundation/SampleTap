'use strict';

angular.module('controllerUxApp')
  .directive('visualswitch', function () {
    return {
      templateUrl: 'views/visualswitch.html',
      restrict: 'E',
      scope: {"switch": "="},
      controller: function($scope) {
    	  $scope.topPorts = $scope.switch.portStatusList;
    	  $scope.bottomPorts = [];
      },
      link: function postLink(scope, element, attrs) {
      }
    };
  });
