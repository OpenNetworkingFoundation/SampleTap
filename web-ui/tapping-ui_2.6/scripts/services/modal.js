'use strict';

angular.module('controllerUxApp')
  .factory('modals', function($q, $location) {
    var service = {
      all: [],
      push: function(type, object) {
        var deferred = $q.defer();
        service.all.push({type: type, object: object, done: function() { deferred.resolve(object)}});
        return deferred.promise;
      },
      pop: function() {
             service.all.pop();
      },
      new: function(type) {
    	  if (type==='tapPolicy') {
    		  $location.path('/wizard/new');
    	  } else {
	          console.log("new modal");
	          var obj = types.new(type);
	          modals.push(type, obj).then(function(o) {
	            backend.addTo(type, o).then(function(o) {
	            	modals.pop();
	            });
	          });
    	  }
      },
      edit: function(type, object) {
    	  if (type==='tapPolicy') {
    		  $location.path('/wizard/' + object.objectId);
    	  } else {
	          var copy = angular.copy(object);
	          modals.push(type, copy).then(function(o) {
	        	  backend.update(type, copy).then(function(o) {
	        		  modals.pop();
	        	  });
	          });
    	  }
      },
      view: function(type, object) {
            var copy = angular.copy(object);
            modals.push(type, copy).then(function(o) {
         	  modals.pop();
           });
      }
    }
    window.modals = service;
    return service;
  });
