package project;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import lib.Debug;

/**
 * A simple HTTP server for supporting external API access.
 * Currently supporting API contexts are following:
 * Note that all message body needs to be JSON form
 *
 * + (POST) /createTenant
 * 		- POST body: {"network" : <network name>}
 * 		- response body : {"method" : "createTenant", "network" : <network name> , "networkId" : <network id created> , "tenantID" : <tenant id created> } on success
 * 						  {"method" : "createTenant", "network" : <network name> , "error" : "true" } on error
 *
 * + (POST) /deleteTenant
 * 		- POST body: {"tenantId" : <tenant id to delete>}
 * 		- response body : {"method" : "deleteTenant", "tenantId" : <tenant id to delete> , "success" : ("true"|"false") }
 *
 * + (POST) /getTenantDetail
 * 		- POST body: {"tenantId" : <tenant id for which to retrieve the detail> }
 * 		- response body : {"method" : "getTenantDetail" , "tenantId" : <tenant id> , "networkId" : <network id> , "subnetId" : (null|<subnet id>) }
 *
 * + (POST) /createSubnet
 * 		- POST body: {"tenantId" : <tenant id> , "networkId" : <network id> , "domain" : <domain name to create for the subnet> }
 * 		- response body : {"method" : "createSubnet" , "tenantId" : <tenant id> , "networkId" : <network id>, "subnetId" : <subnet id> , "domain" : <domain name> } on success
 * 						  {"method" : "createSubnet" , "tenantId" : <tenant id> , "networkId" : <network id>, "error" : "true" } on error
 *
 * + (POST) /destroySubnet
 * 		- POST body : {"tenantId" : <tenant id> , "networkId" : <network id> , "subnetId" : <subnet id> }
 * 		- response body : {"method" : "destroySubnet" , "tenantId" : <tenant id> , "networkId" : <network id>" , "subnetId" : <subnet id> , "success" : ("true"|"false") }
 *
 * + (POST) /getSubnetDetail
 * 		- POST body: {"tenantId" : <tenant id> , "networkId" : <network id> , "subnetId" : <subnet id> }
 * 		- response body : {"method" : "getSubnetDetail" , "tenantId" : <tenant id> , "networkId" : <network id> , "subnetId" : <subnet id> , {stuff from Subnet.getDetail()} ..... } on success
 *						  {"method" : "getSubnetDetail" , "tenantId" : <tenant id> , "networkId" : <network id> , "subnetId" : <subnet id> , "error" : "true" } on error
 *
 * + (POST) /createServer
 * 		- POST body: {"tenantId" : <tenant id> , "networkId" : <network id> , "subnetId" : <subnet id> , "servername" : <server name> , "password" : <password for default user> }
 * 		- response body : {"method" : "createServer" , "tenantId" : <tenant id> , "networkId" : <network id> , "subnetId" : <subnet id> , "serverId" : <server id created> , "servername" : <server name> , "password" : <password> } on success
 *						  {"method" : "createServer" , "tenantId" : <tenant id> , "networkId" : <network id> , "subnetId" : <subnet id> , "error" : "true" } on error
 *
 * + (POST) /destroyServer
 * 		- POST body : {"tenantId" : <tenant id> , "networkId" : <network id> , "subnetId" : <subnet id> , "serverId" : <server id>}
 * 		- response body : {"method" : "destroyServer" , "tenantId" : <tenant id> , "networkId" : <network id>" , "subnetId" : <subnet id> , "serverId" : <server id> , "success" : ("true"|"false") }
 *
 * + (POST) /listServer
 * 		- POST body : {"tenantId" : <tenant id> , "networkId" : <network id> , "subnetId" : <subnet id> }
 * 		- response body : {"method" : "listServer" , "tenantId" : <tenant id> , "networkId" : <network id>" , "subnetId" : <subnet id> , {stuff from APIHandler.listServers()} .... } on success
 * 		                  {"method" : "listServer" , "tenantId" : <tenant id> , "networkId" : <network id>" , "subnetId" : <subnet id> , "error" : "true" } on error
 *
 * + (POST) /getServerDetail
 * 		- POST body: {"tenantId" : <tenant id> , "networkId" : <network id> , "subnetId" : <subnet id> , "serverId" : <server id> }
 * 		- response body : {"method" : "getServerDetail" , "tenantId" : <tenant id> , "networkId" : <network id> , "subnetId" : <subnet id> , "serverId" : <server id> ,{stuff from VirtualServer.getServerDetail()} ..... } on success
 *						  {"method" : "getServerDetail" , "tenantId" : <tenant id> , "networkId" : <network id> , "subnetId" : <subnet id> , "serverId" : <server id> ,"error" : "true" } on error
 *
 * + (POST) /createPort
 * 		- POST body: {"tenantId" : <tenant id> , "networkId" : <network id> , "portNumber" (optional) : <desired port number> }
 * 		- response body : {"method" : "createPort" , "tenantId" : <tenant id> , "networkId" : <network id> , "portNumber" : <assigned port number> } on success
 *						  {"method" : "createPort" , "tenantId" : <tenant id> , "networkId" : <network id> , "portNumber" : <assigned port number> , "error" : "true" } on error
 *
 * + (POST) /destroyPort
 * 		- POST body : {"portNumber" : <port number to free> }
 * 		- response body : {"method" : "destroyPort" , "portNumber" : <freed port> , "success" : ("true"|"false") }
 *
 *
 * + (POST) /linkPort
 * 		- POST body: {"portNumber" : <port number of host to link>  , "downstreamAddress" : <VMs ip address> , "downstreamPort" : <port of VM> , "vnicName" : <name of network interface of vm> }
 * 		- response body : {"method" : "linkPort" , "downstreamAddress" : <VMs ip address>, "downstreamPort" : <port of VM> , "vnicName" : <name of network interface of vm>, "portNumber": <number of port linked> } on success
 *						  {"method" : "linkPort" , "downstreamAddress" : <VMs ip address>, "downstreamPort" : <port of VM> , "vnicName" : <name of network interface of vm> , "error" : "true" } on error
 *
 * + (POST) /unlinkPort
 * 		- POST body: {"portNumber" : <port number of host to unlink>  , "downstreamAddress" : <VMs ip address> , "downstreamPort" : <port of VM> , "vnicName" : <name of network interface of vm> }
 * 		- response body : {"method" : "unlinkPort" , "downstreamAddress" : <VMs ip address>, "downstreamPort" : <port of VM> , "vnicName" : <name of network interface of vm>, "portNumber": <number of port unlinked> } on success
 *						  {"method" : "unlinkPort" , "downstreamAddress" : <VMs ip address>, "downstreamPort" : <port of VM> , "vnicName" : <name of network interface of vm> , "error" : "true" } on error
 *
 *
 * @author TAISHI
 */
public class SimpleHTTPServer {
	private APIHandler apiHandler;
	private HttpServer server;

	private static final int DEFAULT_BACKLOG = 10;

	/**
	 * Binds to the port specified in {@code controller.configMap}
	 * @throws IOException - when failed to start the server
	 */
	public SimpleHTTPServer(Controller controller, APIHandler apiHandler) throws IOException {
		this.apiHandler = apiHandler;

		int ctrlPort = Integer.parseInt(controller.configMap.get("CtrlPort"));

		server = HttpServer.create(new InetSocketAddress(ctrlPort), DEFAULT_BACKLOG );
		server.createContext("/createTenant", new APICallHandler());
		server.createContext("/deleteTenant", new APICallHandler());
		server.createContext("/getTenantDetail", new APICallHandler());

		server.createContext("/createSubnet", new APICallHandler());
		server.createContext("/destroySubnet", new APICallHandler());
		server.createContext("/getSubnetDetail", new APICallHandler());

		server.createContext("/createServer", new APICallHandler());
		server.createContext("/destroyServer", new APICallHandler());
		server.createContext("/listServer", new APICallHandler());
		server.createContext("/getServerDetail", new APICallHandler());

		server.createContext("/createPort", new APICallHandler());
		server.createContext("/destroyPort", new APICallHandler());
		server.createContext("/linkPort", new APICallHandler());
		server.createContext("/unlinkPort", new APICallHandler());

		server.setExecutor(null);
	}

	/**
	 * Starts the HTTP server
	 */
	public void start() {
		server.start();
	}

	private class APICallHandler implements HttpHandler {

		@Override
		public void handle(HttpExchange t) throws IOException {
			String method = t.getRequestMethod();
			if (!method.equals("POST")) {
				//not supporting
				String errorResponse = "non-POST method not allowed";

				byte[] resBytes = errorResponse.getBytes("UTF-8");
				t.sendResponseHeaders(405, resBytes.length);
				OutputStream os = t.getResponseBody();
				os.write(resBytes);
				os.close();
				return;
			}


			String postStr = IOUtils.toString(t.getRequestBody(), "UTF-8");
			JSONObject obj;

			try {
				obj = (JSONObject) JSONValue.parseWithException(postStr);
			} catch (ParseException e) {
				e.printStackTrace();

				//bad request
				String errorResponse = "Bad request -- JSON couldn't be parsed";

				byte[] resBytes = errorResponse.getBytes("UTF-8");
				t.sendResponseHeaders(400, resBytes.length);
				OutputStream os = t.getResponseBody();
				os.write(resBytes);
				os.close();
				return;
			}

			try {
				String api = t.getHttpContext().getPath();
				String response;

				if (api.equals("/createTenant")) {
					response = createTenantHandler(obj);
				}else if (api.equals("/deleteTenant")) {
					response = deleteTenantHandler(obj);
				}else if (api.equals("/getTenantDetail")) {
					response = getTenantDetailHandler(obj);
				}else if (api.equals("/createSubnet")) {
					response = createSubnetHandler(obj);
				}else if (api.equals("/destroySubnet")) {
					response = destroySubnetHandler(obj);
				}else if (api.equals("/getSubnetDetail")) {
					response = getSubnetDetailHandler(obj);
				}else if (api.equals("/createServer")) {
					response = createServerHandler(obj);
				}else if (api.equals("/destroyServer")) {
					response = destroyServerHandler(obj);
				}else if (api.equals("/listServer")) {
					response = listServerHandler(obj);
				}else if (api.equals("/getServerDetail")) {
					response = getServerDetailHandler(obj);
				}else if (api.equals("/createPort")) {
					response = createPortHandler(obj);
				}else if (api.equals("/destroyPort")) {
					response = destroyPortHandler(obj);
				}else if (api.equals("/linkPort")) {
					response = linkPortHandler(obj);
				}else if (api.equals("/unlinkPort")) {
					response = unlinkPortHandler(obj);
				}else {
					//error
					throw new Exception("WEIRD");
				}

				byte[] resBytes = response.getBytes("UTF-8");
				t.sendResponseHeaders(200, resBytes.length);
				OutputStream os = t.getResponseBody();
				os.write(resBytes);
				os.close();
			} catch (IllegalArgumentException e) {
				// bad request
				e.printStackTrace();

				//bad request
				String errorResponse = "Bad request -- JSON couldn't be parsed";

				byte[] resBytes = errorResponse.getBytes("UTF-8");
				t.sendResponseHeaders(400, resBytes.length);
				OutputStream os = t.getResponseBody();
				os.write(resBytes);
				os.close();
				return;
			}catch (Exception e) {
				e.printStackTrace();
				String errorResponse = "Internal Server Error...";

				byte[] resBytes = errorResponse.getBytes("UTF-8");
				t.sendResponseHeaders(500, resBytes.length);
				OutputStream os = t.getResponseBody();
				os.write(resBytes);
				os.close();
			}

		}

		/* API handler -- returns String form of JSON */

		private String createTenantHandler(JSONObject obj) {
			String ntwkName = (String) obj.get("network");
			if (ntwkName == null) {
				throw new IllegalArgumentException();
			}

			HashMap<String,String> response;
			try {
				response = apiHandler.createNewTenantAndNetwork(ntwkName);
				response.put("network", ntwkName);
			} catch (Exception e) {
				if (Debug.IS_DEBUG) {
					e.printStackTrace();
					Debug.debug(e.getMessage());
				}
				//error registering
				response = new HashMap<>();
				response.put("network", ntwkName);
				response.put("error", "true");
			}
			response.put("method", "createTenant");

			return new JSONObject(response).toJSONString();
		}

		private String deleteTenantHandler(JSONObject obj) {
			long tenantId;
			try {
				tenantId = Long.getLong((String) obj.get("tenantId"));
			} catch (Exception e) {
				throw new IllegalArgumentException();
			}

			HashMap<String, String> response = new HashMap<>();
			response.put("method", "deleteTenant");
			response.put("tenantId", Long.toString(tenantId));

			try {
				apiHandler.deleteTenant(tenantId);
				response.put("success", "true");
			} catch (Exception e) {
				if (Debug.IS_DEBUG) {
					e.printStackTrace();
					Debug.debug(e.getMessage());
				}
				response.put("success", "false");
			}


			return new JSONObject(response).toJSONString();
		}

		private String getTenantDetailHandler(JSONObject obj) {
			long tenantId;
			try {
				tenantId = Long.getLong((String) obj.get("tenantId"));
			} catch (Exception e) {
				throw new IllegalArgumentException();
			}

			HashMap<String, String> response = new HashMap<>();
			response.put("method", "getTenantDetail");
			response.put("tenantId", Long.toString(tenantId));

			try {
				response.putAll(apiHandler.getTenantInfo(tenantId));
			} catch (Exception e) {
				if (Debug.IS_DEBUG) {
					e.printStackTrace();
					Debug.debug(e.getMessage());
				}
				response.put("error", "true");
			}

			return new JSONObject(response).toJSONString();
		}

		private String createSubnetHandler(JSONObject obj) {
			long tenantId;
			long networkId;
			String domain;
			try {
				tenantId = Long.getLong((String) obj.get("tenantId"));
				networkId = Long.getLong((String) obj.get("networkId"));
				domain = (String) obj.get("domain");
			} catch (Exception e) {
				throw new IllegalArgumentException();
			}

			if (domain == null) {
				throw new IllegalArgumentException();
			}


			HashMap<String, String> response = new HashMap<>();
			response.put("method", "createSubnet");
			response.put("tenantId", Long.toString(tenantId));
			response.put("networkId", Long.toString(networkId));

			try {
				long subnetId = apiHandler.createNewSubnet(tenantId, networkId, domain);
				response.put("subnetId",Long.toString(subnetId));
				response.put("domain", domain);
			} catch (Exception e) {
				if (Debug.IS_DEBUG) {
					e.printStackTrace();
					Debug.debug(e.getMessage());
				}
				response.put("error", "true");
			}

			return new JSONObject(response).toJSONString();
		}

		private String destroySubnetHandler(JSONObject obj) {
			long tenantId;
			long networkId;
			long subnetId;
			try {
				tenantId = Long.getLong((String) obj.get("tenantId"));
				networkId = Long.getLong((String) obj.get("networkId"));
				subnetId = Long.getLong((String) obj.get("subnetId"));
			} catch (Exception e) {
				throw new IllegalArgumentException();
			}

			HashMap<String, String> response = new HashMap<>();
			response.put("method", "destroySubnet");
			response.put("tenantId", Long.toString(tenantId));
			response.put("networkId", Long.toString(networkId));
			response.put("subnetId", Long.toString(subnetId));

			try {
				apiHandler.destroySubnet(tenantId, networkId, subnetId);
				response.put("success","true");
			} catch (Exception e) {
				if (Debug.IS_DEBUG) {
					e.printStackTrace();
					Debug.debug(e.getMessage());
				}
				response.put("success","false");
			}

			return new JSONObject(response).toJSONString();
		}

		private String getSubnetDetailHandler(JSONObject obj) {
			long tenantId;
			long networkId;
			long subnetId;
			try {
				tenantId = Long.getLong((String) obj.get("tenantId"));
				networkId = Long.getLong((String) obj.get("networkId"));
				subnetId = Long.getLong((String) obj.get("subnetId"));
			} catch (Exception e) {
				throw new IllegalArgumentException();
			}

			HashMap<String,String> response = new HashMap<>();
			response.put("method", "getSubnetDetail");
			response.put("tenantId", Long.toString(tenantId));
			response.put("networkId", Long.toString(networkId));
			response.put("subnetId", Long.toString(subnetId));


			try {
				response.putAll(apiHandler.getSubnetDetails(tenantId, networkId, subnetId));
			} catch (Exception e) {
				if (Debug.IS_DEBUG) {
					e.printStackTrace();
					Debug.debug(e.getMessage());
				}
				response.put("error","true");
			}

			return new JSONObject(response).toJSONString();
		}


		private String createServerHandler(JSONObject obj) {
			long tenantId;
			long networkId;
			long subnetId;
			String servername;
			String password;
			try {
				tenantId = Long.getLong((String) obj.get("tenantId"));
				networkId = Long.getLong((String) obj.get("networkId"));
				subnetId = Long.getLong((String) obj.get("subnetId"));
				servername = (String) obj.get("servername");
				password = (String) obj.get("password");
			} catch (Exception e) {
				throw new IllegalArgumentException();
			}

			if (servername == null || password == null) {
				throw new IllegalArgumentException();
			}

			HashMap<String, String> response = new HashMap<>();
			response.put("method", "createServer");
			response.put("tenantId", Long.toString(tenantId));
			response.put("networkId", Long.toString(networkId));
			response.put("subnetId", Long.toString(subnetId));

			try {
				long serverId = apiHandler.createNewServer(tenantId, networkId, subnetId, servername, password);
				response.put("serverId", Long.toString(serverId));
				response.put("servername", servername);
				response.put("password", password);
			} catch (Exception e) {
				if (Debug.IS_DEBUG) {
					e.printStackTrace();
					Debug.debug(e.getMessage());
				}
				response.put("error","true");
			}

			return new JSONObject(response).toJSONString();
		}

		private String destroyServerHandler(JSONObject obj) {
			long tenantId;
			long networkId;
			long subnetId;
			long serverId;
			try {
				tenantId = Long.getLong((String) obj.get("tenantId"));
				networkId = Long.getLong((String) obj.get("networkId"));
				subnetId = Long.getLong((String) obj.get("subnetId"));
				serverId = Long.getLong((String) obj.get("serverId"));
			} catch (Exception e) {
				throw new IllegalArgumentException();
			}

			HashMap<String,String> response = new HashMap<>();
			response.put("method", "destroyServer");
			response.put("tenantId", Long.toString(tenantId));
			response.put("networkId", Long.toString(networkId));
			response.put("subnetId", Long.toString(subnetId));
			response.put("serverId", Long.toString(serverId));


			try {
				apiHandler.destroyServer(tenantId, networkId, subnetId, serverId);
				response.put("success","true");
			} catch (Exception e) {
				if (Debug.IS_DEBUG) {
					e.printStackTrace();
					Debug.debug(e.getMessage());
				}
				response.put("success","false");
			}

			return new JSONObject(response).toJSONString();
		}

		private String listServerHandler(JSONObject obj) {
			long tenantId;
			long networkId;
			long subnetId;
			try {
				tenantId = Long.getLong((String) obj.get("tenantId"));
				networkId = Long.getLong((String) obj.get("networkId"));
				subnetId = Long.getLong((String) obj.get("subnetId"));
			} catch (Exception e) {
				throw new IllegalArgumentException();
			}

			HashMap<String,String> response = new HashMap<>();
			response.put("method", "listServer");
			response.put("tenantId", Long.toString(tenantId));
			response.put("networkId", Long.toString(networkId));
			response.put("subnetId", Long.toString(subnetId));

			try {
				response.putAll(apiHandler.listServers(tenantId, networkId, subnetId));
			} catch (Exception e) {
				if (Debug.IS_DEBUG) {
					e.printStackTrace();
					Debug.debug(e.getMessage());
				}
				response.put("error","true");
			}

			return new JSONObject(response).toJSONString();
		}

		private String getServerDetailHandler(JSONObject obj) {
			long tenantId;
			long networkId;
			long subnetId;
			long serverId;
			try {
				tenantId = Long.getLong((String) obj.get("tenantId"));
				networkId = Long.getLong((String) obj.get("networkId"));
				subnetId = Long.getLong((String) obj.get("subnetId"));
				serverId = Long.getLong((String) obj.get("serverId"));
			} catch (Exception e) {
				throw new IllegalArgumentException();
			}

			HashMap<String,String> response = new HashMap<>();
			response.put("method", "destroyServer");
			response.put("tenantId", Long.toString(tenantId));
			response.put("networkId", Long.toString(networkId));
			response.put("subnetId", Long.toString(subnetId));
			response.put("serverId", Long.toString(serverId));

			try {
				response.putAll(apiHandler.getServerDetails(tenantId, networkId, subnetId, serverId));
			} catch (Exception e) {
				response.put("error", "true");
			}

			return new JSONObject(response).toJSONString();
		}

		private String createPortHandler(JSONObject obj) {
			long tenantId;
			long networkId;
			int portNum = 0; // generates random port if one not provided
			try {
				tenantId = Long.getLong((String) obj.get("tenantId"));
				networkId = Long.getLong((String) obj.get("networkId"));
				portNum = Integer.parseInt((String) obj.get("portNumber"));
			} catch (Exception e) {
				throw new IllegalArgumentException();
			}

			HashMap<String, String> response = new HashMap<>();
			response.put("method", "createPort");
			response.put("tenantId", Long.toString(tenantId));
			response.put("networkId", Long.toString(networkId));

			try {
				int portNumber = apiHandler.createNewPort(tenantId, networkId, portNum);
				response.put("portNumber", Integer.toString(portNumber));
			} catch (Exception e) {
				if (Debug.IS_DEBUG) {
					e.printStackTrace();
					Debug.debug(e.getMessage());
				}
				response.put("error","true");
			}

			return new JSONObject(response).toJSONString();
		}


		// DESTROY PORT
		private String destroyPortHandler(JSONObject obj) {
			int portNum;
			try {
				portNum = Integer.parseInt((String) obj.get("portNumber"));
			} catch (Exception e) {
				throw new IllegalArgumentException();
			}

			if (portNum == 0){
				throw new IllegalArgumentException();
			}

			HashMap<String, String> response = new HashMap<>();
			response.put("method", "destroyPort");

			try {
				apiHandler.deletePort(portNum);
				response.put("success", "true");
			} catch (Exception e) {
				if (Debug.IS_DEBUG) {
					e.printStackTrace();
					Debug.debug(e.getMessage());
				}
				response.put("success", "false");
			}

			return new JSONObject(response).toJSONString();
		}

		// LINK PORT
		private String linkPortHandler(JSONObject obj) {
			int portNum;
			Inet4Address downstreamAddress;
			int downstreamPort;
			String vnicName = null;
			try {
				portNum = Integer.parseInt((String) obj.get("portNumber"));
				downstreamAddress = (Inet4Address) InetAddress.getByName((String) obj.get("downstreamAddress"));
				downstreamPort = Integer.parseInt((String) obj.get("downstreamPort"));
				vnicName = (String) obj.get("vnicName");
			} catch (Exception e) {
				throw new IllegalArgumentException();
			}

			if (portNum == 0 || downstreamAddress == null || downstreamPort == 0){
				throw new IllegalArgumentException();
			}

			if (vnicName == null){
				throw new IllegalArgumentException();
				//TODO: implement a search for network/vnic by ip/port
			}

			HashMap<String, String> response = new HashMap<>();
			response.put("method", "linkPort");
			response.put("downstreamAddress", downstreamAddress.getHostAddress());
			response.put("downstreamPort", Integer.toString(downstreamPort));
			response.put("vnicName", vnicName);

			try {
				int portNumber = apiHandler.linkPort(portNum, downstreamAddress, downstreamPort, vnicName);
				response.put("portNumber", Integer.toString(portNumber));
			} catch (Exception e) {
				if (Debug.IS_DEBUG) {
					e.printStackTrace();
					Debug.debug(e.getMessage());
				}
				response.put("error","true");
			}

			return new JSONObject(response).toJSONString();
		}

		// unlink port
		private String unlinkPortHandler(JSONObject obj) {
			int portNum;
			Inet4Address downstreamAddress;
			int downstreamPort;
			String vnicName = null;
			try {
				portNum = Integer.parseInt((String) obj.get("portNumber"));
				downstreamAddress = (Inet4Address) InetAddress.getByName((String) obj.get("downstreamAddress"));
				downstreamPort = Integer.parseInt((String) obj.get("downstreamPort"));
				vnicName = (String) obj.get("vnicName");
			} catch (Exception e) {
				throw new IllegalArgumentException();
			}

			if (portNum == 0 || downstreamAddress == null || downstreamPort == 0){
				throw new IllegalArgumentException();
			}

			if (vnicName == null){
				throw new IllegalArgumentException();
				//TODO: implement a search for network/vnic by ip/port
			}

			HashMap<String, String> response = new HashMap<>();
			response.put("method", "unlinkPort");
			response.put("downstreamAddress", downstreamAddress.getHostAddress());
			response.put("downstreamPort", Integer.toString(downstreamPort));
			response.put("vnicName", vnicName);

			try {
				int portNumber = apiHandler.unlinkPort(portNum, downstreamAddress, downstreamPort, vnicName);
				response.put("portNumber", Integer.toString(portNumber));
			} catch (Exception e) {
				if (Debug.IS_DEBUG) {
					e.printStackTrace();
					Debug.debug(e.getMessage());
				}
				response.put("error","true");
			}
			return new JSONObject(response).toJSONString();
		}
	}


}
