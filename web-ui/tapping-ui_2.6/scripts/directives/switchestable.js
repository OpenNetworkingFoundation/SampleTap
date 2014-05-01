'use strict';

angular.module('controllerUxApp')
  .directive('switchesTable', function() {
    return {
      templateUrl: "views/switchestabletemplate.html",
      restrict: 'A',
      scope: {
        switchesTable: '=',
        columns: '=',
        type: '@'
      },
      controller: function($scope) {
                    $scope.backend = $scope.$parent.backend;
                    $scope.modals = $scope.$parent.modals;
                  },
      link: function postLink(scope, element, attrs) {
      }
    };
  });
