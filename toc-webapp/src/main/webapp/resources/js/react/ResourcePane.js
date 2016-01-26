/** @jsx React.DOM */

/**
 * Displays the available resources.
 *
 * TODO: Unit tests.
 */
var ResourcePane = React.createClass({
    getInitialState: function() {
        return {resourceOptions: []}
    },
    render: function() {
        if (this.state.resourceOptions.length > 0) {
            return <div className="resource-list-box ui-widget-content">
                       <RListBox ref="listBox" options={this.state.resourceOptions} selectCallback={this.selectCallback} />
                   </div>
        }
        return <div className="resource-list-box ui-widget-content" style={{padding: "2px 2px"}}><span>Please select a JVM, Web Server or Web Application...</span></div>
    },
    getData: function(data) {
        if (data !== null) {
            if (data.rtreeListMetaData.entity === "jvms") {
                this.props.jvmService.getResources(data.jvmName, this.getDataCallback);
            } else if (data.rtreeListMetaData.entity === "webServers") {
                this.props.wsService.getResources(data.name, this.getDataCallback);
            } else if (data.rtreeListMetaData.entity === "webApps") {
                this.props.webAppService.getResources(data.name, this.getDataCallback);
            } else if (data.rtreeListMetaData.entity === "webServerSection") {
                this.props.groupService.getGroupWebServerResources(data.rtreeListMetaData.parent.name, this.getDataCallback);
            } else if (data.rtreeListMetaData.entity === "jvmSection") {
                this.props.groupService.getGroupJvmResourcesWithAppResources(data.rtreeListMetaData.parent.name, this.getDataCallback);
            }
        }
    },
    getDataCallback: function(response) {
        var options = [];
        response.applicationResponseContent.forEach(function(resourceName){
            if (resourceName.entityType && resourceName.resourceName) {
                options.push({value:resourceName.resourceName, label:resourceName.resourceName, entityType: resourceName.entityType})
            } else {
                options.push({value: resourceName, label: resourceName});
            }
        });
        this.setState({resourceOptions: options});
    },
    selectCallback: function(value) {
         var groupJvmEntityType;
         this.state.resourceOptions.some(function(resource){
            if(resource.value && resource.value === value) {
                groupJvmEntityType = resource.entityType;
                return true;
            }
         });
         return this.props.selectCallback(value, groupJvmEntityType);
    },
    getSelectedValue: function() {
        if (this.refs.listBox !== undefined) {
            return this.refs.listBox.getSelectedValue();
        }
        return null;
    }
});