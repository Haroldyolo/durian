/*
 * Copyright (C) 2015 thirdy
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.thirdy.durian.backend;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import net.thirdy.durian.Main;
import net.thirdy.durian.model.Item;
import net.thirdy.durian.util.FileUtil;

/**
 *
 * @author thirdy
 */
public class BackendApi {

	private static final Logger logger = Logger.getLogger(BackendApi.class.getName());

	public static void setup() {
		Unirest.setDefaultHeader("Authorization", "ed91748a4d2a4f597d69e2b05a058a0a");
//		Unirest.setDefaultHeader("Authorization", "DEVELOPMENT-Indexer");
		Unirest.setDefaultHeader("accept", "application/json");
		// Only one time
		Unirest.setObjectMapper(new ObjectMapper() {
			private com.fasterxml.jackson.databind.ObjectMapper jacksonObjectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

			public <T> T readValue(String value, Class<T> valueType) {
				try {
					return jacksonObjectMapper.readValue(value, valueType);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			public String writeValue(Object value) {
				try {
					return jacksonObjectMapper.writeValueAsString(value);
				} catch (JsonProcessingException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	public static String getAllMappings() {
		String result = "";
		try {
			HttpResponse<String> response = Unirest.get("http://api.exiletools.com/index/_mapping?pretty").asString();
			result = response.getBody();
		} catch (UnirestException ex) {
			logger.log(Level.SEVERE, null, ex);
			throw new BackedException(ex);
		}
		return result;
	}

	static String searchStrings(String url) throws UnirestException {
		HttpResponse<String> response = Unirest.get(url)
				.asString();
		
		return response.getBody();
	}
	public static List<Item> searchUnique(String league, String name, Integer chaosEquiv) {
		List<Item> result = Collections.emptyList();
		try {
			String raw = FileUtil.fromClasspath(BackendApi.class, "query.txt");

			long overlap = 1000 * 60 * 10;
			long millisBack = (long)Main.DURATION.toMillis() + overlap ;
			long modifiedGte = System.currentTimeMillis() - millisBack;
			raw = raw.replace("$MODIFIED_GTE", String.valueOf(modifiedGte));
			raw = raw.replace("$LEAGUE", league);
			raw = raw.replace("$RARITY", "Unique");
			raw = raw.replace("$NAME", name);
			raw = raw.replace("$PRICE_LESS_THAN_CHAOS", String.valueOf(chaosEquiv));
			HttpResponse<JsonNode> response = Unirest.post("http://api.exiletools.com/index/_search?pretty")
					.body(raw).asJson();
			
			logger.info(String.format("Request: %s|%s|%s - %s -- HTTP POST Status Response: %s",
					league,
					name,
					chaosEquiv,
					new Date(modifiedGte),
					response.getStatusText()));
			
			JSONObject jsonObject = response.getBody().getObject();
			
			JSONArray jsonArray = jsonObject.getJSONObject("hits").getJSONArray("hits");

			result = new ArrayList<>(jsonArray.length());
			// TODO, find a better way to parse yet another file format, why can't people just use lisp?
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject fieldsJsonObject = jsonArray.getJSONObject(i).getJSONObject("fields");
				JSONArray partial1Array = fieldsJsonObject.getJSONArray("partial1");
				JSONObject partial1Object = partial1Array.getJSONObject(0);
				JSONObject shopObject = partial1Object.getJSONObject("shop");
				JSONObject infoObject = partial1Object.getJSONObject("info");
				JSONObject attributesObject = partial1Object.getJSONObject("attributes");
				Item item = new Item();
				item.setAmount(shopObject.getDouble("amount"));
				item.setSellerIGN(shopObject.getString("sellerIGN"));
				item.setSellerAccount(shopObject.getString("sellerAccount"));
				item.setThreadid(shopObject.getString("threadid"));
				item.setCurrency(shopObject.getString("currency"));
				item.setIcon(infoObject.getString("icon"));
				item.setName(name);
				item.setModified(new Date(Long.parseLong(shopObject.get("modified").toString())).toString());
				item.setUuid(partial1Object.getString("uuid"));
				item.setLeague(attributesObject.getString("league"));
				result.add(item);
			}

		} catch (URISyntaxException | IOException | UnirestException ex) {
			logger.log(Level.SEVERE, null, ex);
			throw new BackedException(ex);
		}

		return result;
	}

	public static void shutdown() {
		try {
			Unirest.shutdown();
		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
			throw new BackedException(ex);
		}
	}

	public static String post(String url, String body) throws JSONException, UnirestException {
		HttpResponse<JsonNode> asJson = Unirest.post(url)
				.body(body).asJson();
		return asJson.getBody().getObject().toString(1);
	}

}
