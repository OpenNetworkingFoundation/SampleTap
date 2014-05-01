'use strict';

angular.module('controllerUxApp')
  .directive('logs', function($rootScope) {
    return {
      templateUrl: "views/logstemplate.html",
      restrict: 'A',
      scope: {},
      controller: function($scope, backend) {
                  $scope.backend = $scope.$parent.backend;
                  $scope.showTab = function(tabName) {
                  	return tabName == $scope.backend.bottomTab;
                  };
                  $scope.setTab = function(tab) {
                  	$scope.backend.bottomTab = tab;
                  };

                  $scope.columns = function(opfSwitch) {
                    var keys = Object.keys(opfSwitch.switchPortStatistics[0]);
                    keys.pop();
                    return keys
                  };
      },
      link: function postLink(scope, element, attrs) {
    	  $rootScope.$on("scrollDownLogs", function (e) {
    		  var logsPane = element.find("#logs-pane")
    		  window.test = logsPane;
    		  logsPane.scrollTop(logsPane[0].scrollHeight);
    	  });
      }
    };
  });
