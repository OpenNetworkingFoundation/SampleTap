'use strict';

angular.module('controllerUxApp')
  .factory('types', function (backend) {

    // lists
    
    var from = function(obj, key) {
      var f = function() {
        console.log("evaluating source fn");
        return obj[key];
      };
      f.type = "from";
      return f;
    }

    var constantly = function(x) {
      return function() {
        return x;
      };
    };


    var fields = {
      tapPolicy: [
        ["name", "string"],
        ["description", "string"],
        ["matchCriteriaIdList", "selectManyObjects", 'matchCriteria'],
        ["switchAndPortList", "embedded", 'switchAndPort'],
        ['captureDevIdList', 'selectManyObjects', 'captureDevice'],
        ['portChainIdList', 'selectManyObjects', 'portChain'],
        ["enabled", "boolean"]
      ],
      switchEntry: [
        ["name", "string"],
        ["tappingMode", "select", constantly(["TAPAGGR"])],
        ["opfSwitch", "selectOPFSwitch", "opfSwitch"]
      ],
      nextHopSwitch: [
        ["switchName", "string"],
        ["nextSwitchId", "selectObject", "switchEntry"],
        ["VLANTag", "string"]
      ],
      opfSwitch: [
        ["ipAddressDesc", 'string'],
        ["description", 'string'],
        ["MACAddress", 'string'],
        ["MFRDescription", 'string'],
        ["HardwareDesc", "string",],
        ["SoftwareDesc","string"],
        ["SerialNumber","string"],
        ["DataPathDesc","string"]
      ],
      captureDevice: [
        ['name', 'string'],
        ['captureType', 'select', constantly(["IDS","RECORDER","ANALYZER","PACKET_BROKER"])],
        ['switchId', 'selectObject', 'switchEntry'],
        ['switchPort', 'string'],
        ['ipAddr', 'string'],
        ['vlanId', 'string'],
        ['macAddr', 'string'],
        ['enabled', 'boolean']
      ],
      portChain: [
        ['name', 'string'],
        ['type', 'string'],
        ['outPort', 'embedOne', 'switchAndPort'],
        ['returnPort', 'embedOne', 'switchAndPort'],
        ['outRewrite', 'embedOne', 'rewriteParams'],
        ['returnRewrite', 'embedOne', 'rewriteParams']
      ],
      switchAndPort: [
        ['name', 'string'],
        ['switchId', 'selectObject', 'switchEntry'],
        ['switchPort', 'string']
      ],
      rewriteParams: [
        ['name', 'string'],
        ['ipAddress', 'string'],
        ['vlanId', 'string'],
        ['macAddr', 'string'],
      ],
      matchCriteria: [
        ['name', 'string'],
        ['reflexive', 'boolean'],
        ['priority', 'string'],
        ['matchFieldList', 'embedded', 'matchField']
      ],
      matchField: [
        ['type', 'select', constantly(["ETHER_TYPE", 
                                       "IP_PROTO", "IP_TOS", "VLAN_TAG", 
                                       "SOURCE_IP", "SOURCE_MAC", "DEST_IP", 
                                       "DEST_MAC", "PROTO_SVC", "PROTO_PORT"])],
        ['value', 'string']
      ]
    }

    var service = {
      new: function(type) {
             var result = {};
             fields[type].forEach(function(field) {
            	console.log("building", field[0], "for", type)
            	if (field[1] == "embedded" || field[1] == "selectMany" || field[1] == "selectManyObjects") {
            		result[field[0]] = [];
            	}
            	else if (field[1] == "embedOne") {
            		console.log("embedding one", field[0], "in", type)
            		result[field[0]] = service.new(field[2]);
            	}
            	else if (field[0] == "enabled") {
            		result[field[0]] = true;
            	}
            	else {
                    result[field[0]] = null;
            	}
             })
             return result;
           },
      fields: fields
    };

    window.types = service;

    return service;
  });
