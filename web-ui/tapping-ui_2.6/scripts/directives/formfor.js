'use strict';

angular.module('controllerUxApp')
  .directive('formFor', function (RecursionHelper) {
    return {
      templateUrl: 'views/form.html',
      restrict: 'A',
      scope: {object: '=formFor', type: '='},
      controller: function ($scope, types, backend) {
    	  $scope.types = types;
    	  $scope.typeof = function(obj) {
    	      return typeof(obj);
    	    };
    	  $scope.backend = backend;
      },
      compile: function(element){
    	  return RecursionHelper.compile(element);
      }
    };
  });
