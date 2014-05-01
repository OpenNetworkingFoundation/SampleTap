'use strict';

angular.module('controllerUxApp')
  .directive('tableData', function() {
    return {
      templateUrl: "views/tabletemplate.html",
      restrict: 'A',
      scope: {
        tableData: '=',
        columns: '=',
        type: '@',
        heading: '@'
      },
      controller: function($scope) {
                    $scope.modals = $scope.$parent.modals;
                    $scope.backend = $scope.$parent.backend;
                  },
      link: function postLink(scope, element, attrs) {
      }
    };
  });
