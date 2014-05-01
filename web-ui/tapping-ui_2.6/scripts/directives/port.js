'use strict';

angular.module('controllerUxApp')
  .directive('port', function () {
    return {
      templateUrl: 'views/port.html',
      restrict: 'A',
      scope: { port: '=' },
      link: function postLink(scope, element, attrs) {
      }
    };
  });
