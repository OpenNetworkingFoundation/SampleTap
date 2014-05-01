'use strict';

angular.module('controllerUxApp')
  .factory('backend', function backend($q, $timeout, $http, $rootScope) {
	var convertNumsToStrings = function(o) {
		for (var i in o) {
		   	if (o[i] !== null) {
		   		switch (typeof(o[i]))
		   		{
		   		case 'number':
		   			o[i] = o[i].toString();
		   			break;
		   		case 'object':
		   			convertNumsToStrings(o[i]);
		   			break;
		   		}
		    }
		}
	};
    var resource = function(url) {
    	var r = {
    		all: [],
    		ids: function() {
    			return r.all.map(function(o) { return o.objectId; })
    		},
    		dataPathDescs: function() {
    			return r.all.map(function(o) { return o.DataPathDesc; })
    		},
    		refresh: function(callback) {
    			//Refresh object
    			$http.get(url).then(function(response){
    				console.log("refreshed", url, response)
    				convertNumsToStrings(response.data);
    	    		r.all = response.data;
        			
    	    		//Refresh Logs
        			$http.get("/logs").then(function(response){
        				console.log("refreshed logs", response);
        	    		service.logs.all = response.data;
        	    		$timeout(function() { 
                                        if (service.bottomTab == 'logs')
        	    			  $rootScope.$emit("scrollDownLogs");
        	    			if (callback)
        	    				callback();
        	    		}, 100);
        			});
    	    	        });
    		},
    		addTo: function(obj) {
    			return $http.post(url, obj).then(function(response){
    				r.refresh();
    				return obj;
    			})
    		},
    		delete: function(obj) {
    			return $http.delete(url + '/' + obj.objectId).then(function(response){
    				r.refresh();
    				return response;
    			})
    		},
    		update: function(obj) {
    			return $http.put(url + '/' + obj.objectId, obj).then(function(response){
    				r.refresh();
    				return response;
    			})
    		},
    		lookup: function(id) {
    			for(var i = 0; i < r.all.length; i++) {
    				if (r.all[i].objectId == id) {
    					console.log("looking up", id, "and returning", r.all[i])
    					return r.all[i];
    				}
    			}
    			return null;
    		}
    	}
    	r.refresh();
    	return r;
    };


    var service = {
      opfSwitch: resource("/opfswitches"),
      tapPolicy: resource("/tappolicies"),
      matchCriteria: resource("/matchcriteria"),
      nextHopSwitch: resource("/nexthopswitches"),
      switchEntry: resource("/switchentries"),
      captureDevice: resource("/capturedevices"),
      portChain: resource("/portchains"),
      logs: resource("/logs"),
      filters: [],
      remove: function(type, obj) {
        service[type].delete(obj);
      },
      addTo: function(type, obj) {
        return service[type].addTo(obj);
      },
      update: function(type, obj) {
    	  return service[type].update(obj);
      },
      lookup: function(type, id) {
    	  return service[type].lookup(id);
      },
      bottomTab: 'logs'
    }
    window.backend = service;
       
    return service;
  });
