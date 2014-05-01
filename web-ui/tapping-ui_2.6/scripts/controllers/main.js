'use strict';

angular.module('controllerUxApp')
  .controller('MainCtrl', function ($scope, backend, modals, types, $timeout) {
    $scope.backend = backend;
    $scope.modals = modals;
    $scope.types = types;
    
    (function tick() {
        $scope.backend.opfSwitch.refresh(function(){
            $timeout(tick, 1000);
            console.log('updating opfSwich');
        });
    })();
  });
