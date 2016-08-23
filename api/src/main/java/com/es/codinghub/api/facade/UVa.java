package com.es.codinghub.api.facade;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.es.codinghub.api.entities.Submission;
import com.es.codinghub.api.entities.Verdict;

public class UVa extends OnlineJudge {

	private String userID;

	private static Map<Integer, String> problemsByID;
	private static Map<Integer, String> problemsByNumber;

	private static JSONArray cpBook;

	public UVa(String username) throws IOException {
		super("http://uhunt.felix-halim.net/api");
		this.userID = request("/uname2uid/" + username);

		if (problemsByID == null || problemsByNumber == null)
			cacheProblems();
	}

	@Override
	public JSONArray getSubmissionsAfter(Submission last) throws IOException {
		String response = (last == null)?
			request("/subs-user/" + userID):
			request("/subs-user/" + userID + "/" + last.getId());

		JSONArray subs = new JSONObject(response).getJSONArray("subs");
		JSONArray result = new JSONArray();

		for (int i = 0; i < subs.length(); ++i) {
			JSONArray sub = subs.getJSONArray(i);
			result.put(createSubmission(sub));
		}

		return result;
	}

	@Override
	public JSONArray getSudgestedProblems() {
		return cpBook;
	}

	public static void main(String[] args) {
		try {
			UVa uva = new UVa("VictorAA");
			System.out.println("Requesting submissions...");

			JSONArray result = uva.getSubmissionsAfter(null);
			System.out.println(result);

			result = uva.getSudgestedProblems();
			System.out.println(result);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void cacheProblems() throws IOException {
		problemsByID = new HashMap<>();
		problemsByNumber = new HashMap<>();

		String response = request("/p");
		JSONArray probs = new JSONArray(response);

		for (int i = 0; i < probs.length(); ++i) {
			JSONArray prob = probs.getJSONArray(i);
			String title = prob.getInt(1) + " - " + prob.getString(2);

			problemsByID.put(prob.getInt(0), title);
			problemsByNumber.put(prob.getInt(1), title);
		}

		response = request("/cpbook/3");
		cpBook = parseBook(new JSONArray(response));
	}

	private Submission createSubmission(JSONArray sub) {
		return new Submission(
			sub.getInt(0),
			sub.getInt(4),
			problemsByID.get(sub.getInt(1)),
			mapVerdict(sub.getInt(2))
		);
	}

	private Verdict mapVerdict(int ver) {
		switch (ver) {
			case 30: return Verdict.COMPILATION_ERROR;
			case 40: return Verdict.RUNTIME_ERROR;
			case 50: return Verdict.TIME_LIMIT;
			case 60: return Verdict.MEMORY_LIMIT;
			case 70: return Verdict.WRONG_ANSWER;
			case 80: return Verdict.PRESENTATION_ERROR;
			case 90: return Verdict.ACCEPTED;
			default: return Verdict.OTHER;
		}
	}

	private JSONArray parseBook(JSONArray book) {
		JSONArray result = new JSONArray();

		for (Object e : book) {
			String tag = null;
			JSONArray elems = null;

			if (e instanceof JSONObject) {
				JSONObject obj = (JSONObject) e;

				tag = obj.getString("title");
				elems = parseBook(obj.getJSONArray("arr"));
			}

			if (e instanceof JSONArray) {
				JSONArray arr = (JSONArray) e;
				JSONArray probs = new JSONArray();

				for (int i=1; i<arr.length(); ++i) {
					int number = Math.abs(arr.getInt(i));
					probs.put(problemsByNumber.get(number));
				}

				tag = arr.getString(0);
				elems = probs;
			}

			JSONObject sub = new JSONObject();

			sub.put("tag", tag);
			sub.put("elements", elems);

			result.put(sub);
		}

		return result;
	}
}
