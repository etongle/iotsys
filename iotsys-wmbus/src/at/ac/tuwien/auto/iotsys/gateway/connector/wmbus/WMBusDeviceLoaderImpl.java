/*******************************************************************************
 * Copyright (c) 2013
 * Institute of Computer Aided Automation, Automation Systems Group, TU Wien.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * 
 * This file is part of the IoTSyS project.
 ******************************************************************************/

package at.ac.tuwien.auto.iotsys.gateway.connector.wmbus;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import obix.Obj;
import obix.Uri;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

import at.ac.tuwien.auto.iotsys.commons.Connector;
import at.ac.tuwien.auto.iotsys.commons.DeviceLoader;
import at.ac.tuwien.auto.iotsys.commons.ObjectBroker;
import at.ac.tuwien.auto.iotsys.commons.PropertiesLoader;

public class WMBusDeviceLoaderImpl implements DeviceLoader {
	
	private static Logger log = Logger.getLogger(WMBusDeviceLoaderImpl.class
			.getName());

	private XMLConfiguration devicesConfig = new XMLConfiguration();
	
	private ArrayList<String> myObjects = new ArrayList<String>();

	public WMBusDeviceLoaderImpl() {
		
		String devicesConfigFile = DEVICE_CONFIGURATION_LOCATION;

		try {
			devicesConfig = new XMLConfiguration(devicesConfigFile);
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	@Override
	public ArrayList<Connector> initDevices(ObjectBroker objectBroker) {
		ArrayList<Connector> connectors = new ArrayList<Connector>();

		int connectorsSize = 0;
		// WMBus
		Object wmbusConnectors = devicesConfig
				.getProperty("wmbus.connector.name");
		if (wmbusConnectors != null) {
			connectorsSize = 1;
		} else {
			connectorsSize = 0;
		}

		if (wmbusConnectors instanceof Collection<?>) {
			connectorsSize = ((Collection<?>) wmbusConnectors).size();
		}

		for (int connector = 0; connector < connectorsSize; connector++) {
			HierarchicalConfiguration subConfig = devicesConfig
					.configurationAt("wmbus.connector(" + connector + ")");

			Object wmbusConfiguredDevices = subConfig
					.getProperty("device.type");
			String connectorName = subConfig.getString("name");
			String serialPort = subConfig.getString("serialPort");
			Boolean enabled = subConfig.getBoolean("enabled", false);

			if (enabled) {
				try {
					WMBusConnector wmbusConnector = new WMBusConnector(
							serialPort);
					wmbusConnector.connect();
					connectors.add(wmbusConnector);

					int wmbusDevicesCount = 0;
					if (wmbusConfiguredDevices instanceof Collection<?>) {
						Collection<?> wmbusDevice = (Collection<?>) wmbusConfiguredDevices;
						wmbusDevicesCount = wmbusDevice.size();

					} else if (wmbusConfiguredDevices != null) {
						wmbusDevicesCount = 1;
					}

					log.info(wmbusDevicesCount
							+ " WMBus devices found in configuration for connector "
							+ connectorName);

					for (int i = 0; i < wmbusDevicesCount; i++) {
						String type = subConfig.getString("device(" + i
								+ ").type");
						List<Object> address = subConfig.getList("device(" + i
								+ ").address");
						String ipv6 = subConfig.getString("device(" + i
								+ ").ipv6");
						String href = subConfig.getString("device(" + i
								+ ").href");

						Boolean historyEnabled = subConfig.getBoolean("device("
								+ i + ").historyEnabled", false);

						Integer historyCount = subConfig.getInt("device(" + i
								+ ").historyCount", 0);

						if (type != null && address != null) {
							String serialNr = (String) address.get(0);
							String aesKey = (String) address.get(1);

							Object[] args = new Object[3];
							args[0] = wmbusConnector;
							args[1] = serialNr;
							args[2] = aesKey;

							try {

								Constructor<?>[] declaredConstructors = Class
										.forName(type)
										.getDeclaredConstructors();
								for (int k = 0; k < declaredConstructors.length; k++) {
									if (declaredConstructors[k]
											.getParameterTypes().length == args.length) { // constructor
																							// that
																							// takes
																							// the
																							// KNX
																							// connector
																							// and
																							// group
																							// address
																							// as
																							// argument
										Obj smartMeter = (Obj) declaredConstructors[k]
												.newInstance(args); // create
																	// a
																	// instance
																	// of
																	// the
																	// specified
																	// KNX
																	// device
										smartMeter.setHref(new Uri(href));
										
										ArrayList<String> assignedHrefs = null;
										if (ipv6 != null) {
											assignedHrefs = objectBroker.addObj(smartMeter,
													ipv6);
										} else {
											assignedHrefs = objectBroker.addObj(smartMeter);
										}
										
										synchronized(myObjects){
											myObjects.addAll(assignedHrefs);
										}
										smartMeter.initialize();
									
										if (historyEnabled != null
												&& historyEnabled) {
											if (historyCount != null
													&& historyCount != 0) {
												objectBroker
														.addHistoryToDatapoints(
																smartMeter,
																historyCount);
											} else {
												objectBroker
														.addHistoryToDatapoints(smartMeter);
											}
										}
									}
								}
							} catch (SecurityException e) {
								e.printStackTrace();
							} catch (ClassNotFoundException e) {
								e.printStackTrace();
							}
						}
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return connectors;
	}

	@Override
	public void removeDevices(ObjectBroker objectBroker) {
		synchronized(myObjects){
			for(String href : myObjects){
				objectBroker.removeObj(href);
			}
		}
	}
}