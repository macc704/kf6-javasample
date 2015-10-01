package kf6connector;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

@SuppressWarnings("unused")
public class App {

	private Gson gson;
	private String server = "localhost:9000";
	private String baseURL = "http://" + server;
	private HttpClient client;
	private Token token;
	private String communityId;

	public void run() throws Exception {
		gson = new GsonBuilder().create();
		client = HttpClientBuilder.create().build();

		{// login
			HttpPost req = new HttpPost(baseURL + "/auth/local");
			Login login = new Login();
			login.email = "XXXX";
			login.password = "xxxx";
			StringEntity entity = new StringEntity(gson.toJson(login), StandardCharsets.UTF_8);
			req.addHeader("Content-type", "application/json");
			req.setEntity(entity);
			HttpResponse res = client.execute(req);

			String content = EntityUtils.toString(res.getEntity());
			token = gson.fromJson(content, Token.class);
		}

		{// regsitrations
			HttpGet req = new HttpGet(baseURL + "/api/users/myRegistrations");
			req.setHeader("authorization", "Bearer " + token.token);
			HttpResponse res = client.execute(req);
			String content = EntityUtils.toString(res.getEntity());
			List<KAuthor> authors = gson.fromJson(content, new TypeToken<List<KAuthor>>() {
			}.getType());
			for (KAuthor author : authors) {
				System.out.print(author._community.title);
				System.out.print(", ");
				System.out.print(author._id);
				System.out.print(", ");
				System.out.print(author.communityId);
				System.out.println();
			}
		}

		communityId = "54eb7a7b322928d86de4631c";// kf17

		{// list of notes
			HttpPost req = new HttpPost(baseURL + "/api/contributions/" + communityId + "/search");
			req.setHeader("authorization", "Bearer " + token.token);
			Search search = new Search();
			search.query.communityId = communityId;
			search.query.pagesize = "10000";
			StringEntity entity = new StringEntity(gson.toJson(search), StandardCharsets.UTF_8);
			req.addHeader("Content-type", "application/json");
			req.setEntity(entity);
			HttpResponse res = client.execute(req);
			String content = EntityUtils.toString(res.getEntity());
			// prettyPrint(content);
		}

		{// list of views
			HttpGet req = new HttpGet(baseURL + "/api/communities/" + communityId + "/views");
			req.setHeader("authorization", "Bearer " + token.token);
			HttpResponse res = client.execute(req);
			String content = EntityUtils.toString(res.getEntity());
			// prettyPrint(content);
		}

		{// scaffold tracking
			HttpGet req = new HttpGet(baseURL + "/api/communities/" + communityId);
			req.setHeader("authorization", "Bearer " + token.token);
			HttpResponse res = client.execute(req);
			String content = EntityUtils.toString(res.getEntity());
			KCommunity community = gson.fromJson(content, KCommunity.class);
			for (String scaffoldId : community.scaffolds) {
				KObject obj = getObject(scaffoldId);
				List<KLink> links = getLinksByFrom(obj._id);
				for (KLink link : links) {
					KObject support = getObject(link.to);
					// prettyPrint(support);
					processSupport(support);
					// System.out.println(link.data.order);
				}
			}
		}
	}

	private void processSupport(KObject support) throws Exception {
		List<KLink> links = getLinksByFrom(support._id);
		System.out.print(support.title);
		System.out.print(":");
		System.out.print(links.size());
		System.out.println();
		// for (KLink link : links) {
		// KObject note = getObject(link.to);
		// processSupport(note);
		// }
	}

	private List<KLink> getLinksByTo(String id) throws Exception {
		return getLinksByFrom(id, "to");
	}

	private List<KLink> getLinksByFrom(String id) throws Exception {
		return getLinksByFrom(id, "from");
	}

	private List<KLink> getLinksByFrom(String id, String direction) throws Exception {
		HttpGet req = new HttpGet(baseURL + "/api/links/" + direction + "/" + id);
		req.setHeader("authorization", "Bearer " + token.token);
		HttpResponse res = client.execute(req);
		String content = EntityUtils.toString(res.getEntity());
		List<KLink> links = gson.fromJson(content, new TypeToken<List<KLink>>() {
		}.getType());
		return links;
	}

	class KLink {
		String _id;
		String to;
		String from;
		String type;
		KLinkData data;
	}

	class KLinkData {
		String order;
	}

	private KObject getObject(String id) throws Exception {
		HttpGet req = new HttpGet(baseURL + "/api/objects/" + id);
		req.setHeader("authorization", "Bearer " + token.token);
		HttpResponse res = client.execute(req);
		String content = EntityUtils.toString(res.getEntity());
		KObject object = gson.fromJson(content, KObject.class);
		return object;
	}

	class KObject {
		String _id;
		String title;
		String type;
		JsonObject data;
	}

	private void prettyPrint(Object obj) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String prettyJsonString = gson.toJson(obj);
		System.out.println(prettyJsonString);
	}

	private void prettyPrint(String jsonString) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonParser jp = new JsonParser();
		JsonElement je = jp.parse(jsonString);
		String prettyJsonString = gson.toJson(je);
		System.out.println(prettyJsonString);
	}

	class Login {
		String email;
		String password;
	}

	class Token {
		String token;
	}

	class Search {
		Query query = new Query();
	}

	class Query {
		String communityId;
		List<String> viewIds;
		String pagesize;
	}

	class KAuthor {
		String _id;
		String userId;
		String communityId;
		String type;
		String role;
		String firstName;
		String lastName;
		KCommunity _community;
	}

	class KCommunity {
		String _id;
		String title;
		List<String> scaffolds;
		List<String> views;
	}

}
