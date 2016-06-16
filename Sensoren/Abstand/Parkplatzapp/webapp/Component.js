sap.ui.define([
	"sap/ui/core/UIComponent",
	"sap/ui/Device",
	"generated/app/model/models"
], function(UIComponent, Device, models) {
	"use strict";

	var navigationWithContext = {

	};

	return UIComponent.extend("generated.app.Component", {

		metadata: {
			manifest: "json"
		},

		/**
		 * The component is initialized by UI5 automatically during the startup of the app and calls the init method once.
		 * @public
		 * @override
		 */
		init: function() {
			// set the device model
			this.setModel(models.createDeviceModel(), "device");
			// set the FLP model
			this.setModel(models.createFLPModel(), "FLP");

			// call the base component's init function
			UIComponent.prototype.init.apply(this, arguments);

			// create the views based on the url/hash
			this.getRouter().initialize();
		},

		createContent: function() {
			var app = new sap.m.App({
				id: "App"
			});
			var appType = "App";
			if (appType === "App") {
				app.setBackgroundColor("#FFFFFF");
			}

			return app;
		},

		getNavigationPropertyForNavigationWithContext: function(entityNameSet, targetPageName) {
			var entityNavigations = navigationWithContext[entityNameSet];
			return entityNavigations == null ? null : entityNavigations[targetPageName];
		}
	});

});