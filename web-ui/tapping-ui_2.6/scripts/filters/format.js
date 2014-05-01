'use strict';

angular.module('controllerUxApp')
  .filter('format', function () {
    return function (input) {
      if (typeof(input) == "boolean") {
        return input ? '✓' : '✗' ;
      }
      else {
        return input;
      }
    };
  });
