package tw.alan.chatbot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;

import org.json.JSONArray;
import org.json.JSONObject;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;

public class App extends JFrame {

	private JButton sendbutton;
	private JTextField TextField;
	private JTextArea TextArea; // 可改用JTextPane 可以放icon
	private String answer;

	public App() {
		super("chatbot");

		sendbutton = new JButton("send");
		TextField = new JTextField();
		TextArea = new JTextArea();

		setLayout(new BorderLayout());
		JPanel bottomboard = new JPanel(new BorderLayout());
		add(bottomboard, BorderLayout.SOUTH);
		bottomboard.add(sendbutton, BorderLayout.EAST);
		bottomboard.add(TextField, BorderLayout.CENTER);

		TextArea.setBackground(Color.BLACK);
		TextField.setBackground(Color.WHITE);

		JScrollPane jsp = new JScrollPane(TextArea);
		add(jsp, BorderLayout.CENTER);

		setSize(640, 480);
		setVisible(true);
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		TextField.setFont(new Font(null, Font.BOLD, 18));
		TextArea.setFont(new Font(null, Font.BOLD, 18));

		sendbutton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				if (e.getSource() == sendbutton) { // 傳送訊息的按鈕

					final String inputtext = TextField.getText().toLowerCase();
					TextArea.setForeground(Color.GREEN);
					TextArea.append("You : " + inputtext + "\n");
					TextField.setText("");

					// 按鈕播放音效
					String soundFilePath = "sound/but2.wav";
					try {
						File soundFile = new File(soundFilePath);
						AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);
						Clip clip = AudioSystem.getClip();
						clip.open(audioInputStream);
						clip.start();
					} catch (IOException | UnsupportedAudioFileException | LineUnavailableException ex) {
						ex.printStackTrace();
					}

					Timer timer = new Timer(10, new ActionListener() { // timer 逐字出現
						private int currentIndex = 0;
						private String response = getResponse(inputtext);

						@Override
						public void actionPerformed(ActionEvent e) {
							if (currentIndex < response.length()) {
								TextArea.append(String.valueOf(response.charAt(currentIndex)));
								currentIndex++;
							} else {
								((Timer) e.getSource()).stop();
								TextArea.append("\n"); // 換行
							}
						}

					});
					timer.start();

				}
			}
		});

	}

	private static String getResponse(String input) {
		String[] greetings = { "你好", "嗨", "HELLO", "HI" }; // 未來想增加可以從輸入 新增判斷進來 可能不能使用陣列
		String[] weathers = { "天氣", "氣象", "氣溫", "WEATHER" };
		String[] itembases = { "倉庫", "庫存", "SELECT", "LEFT", "BASE" };
		String[] qitembases = { "新增詢問", "ADDDATE" };
		String[] drawlots = { "抽籤", "運氣", "DRAWLOTS", "ROLL" };

		String greetingresponse = "你好，有什麼我可以幫助你?";
		String qitembasesresponse = "你好，已幫你新增資料完畢";

		List<Term> terms = HanLP.segment(input); // 進行分詞

		String[] words = new String[terms.size()]; // 提取分詞後的詞語
		for (int i = 0; i < terms.size(); i++) {
			words[i] = terms.get(i).word;
		}

		String[] lowercaseWords = new String[words.length]; // 將詞語轉換為小寫
		for (int i = 0; i < words.length; i++) {
			lowercaseWords[i] = words[i].toLowerCase();
		}

		boolean containsGreeting = false; // 比對輸入文字 問候
		for (String greeting : greetings) {
			if (Arrays.asList(lowercaseWords).contains(greeting.toLowerCase())) { // 比對小寫與問候語
				containsGreeting = true;
				break;
			}
		}

		boolean containsweather = false; // 比對輸入文字 天氣查詢
		for (String weather : weathers) {
			if (Arrays.asList(lowercaseWords).contains(weather.toLowerCase())) {
				containsweather = true;
				break;
			}
		}

		boolean containitembase = false; // 比對輸入文字 查詢資料庫
		for (String itembase : itembases) {
			if (Arrays.asList(lowercaseWords).contains(itembase.toLowerCase())) {
				containitembase = true;
				break;
			}
		}

		boolean containqitembases = false; // 比對輸入文字 輸入資料庫
		for (String qitembase : qitembases) {
			if (Arrays.asList(lowercaseWords).contains(qitembase.toLowerCase())) { // 比對小寫與問候語
				containqitembases = true;

				break;
			}
		}

		boolean containdrawlots = false; // 比對輸入文字 抽籤
		for (String drawlot : drawlots) {
			if (Arrays.asList(lowercaseWords).contains(drawlot.toLowerCase())) {
				containdrawlots = true;
				break;
			}
		}

		// 模擬對答邏輯

		if (containsGreeting) {
			return greetingresponse;

		} else if (containsweather) {
			getWeatherdate();
			String ansr = "以下是為你查詢近36小時的天氣資訊 :D \n" + getWeatherdate();
			return ansr;

		} else if (containitembase) {

			String a = ""; // 要先初始化
			try {
				Connection conn = connectionDatebase();
				a = selectDatabase(conn);
			} catch (Exception e) {
				System.out.println(e);
			}
			return a;

		} else if (containqitembases) {
			try {
				Connection conn = connectionDatebase();
				insertDatabase(conn, lowercaseWords[1], lowercaseWords[2], lowercaseWords[3], lowercaseWords[4],
						lowercaseWords[5]);
			} catch (Exception e) {
				System.out.println(e);
			}

			return qitembasesresponse;

		}

		else if (containdrawlots) {

			String a = ""; // 要先初始化
			try {
				Connection conn = connectionDatebase();
				a = "恰恰-!~ 祝你好運:D 以下為抽籤結果:\n" + drawlots(conn);
			} catch (Exception e) {
				System.out.println(e);
			}
			return a;

		} else {
			return "抱歉，我不太理解你說的話。";
		}
	}

	static Connection connectionDatebase() throws Exception { // 連接資料庫
		Properties prop = new Properties();
		prop.put("user", "root");
		prop.put("password", "root");
		Class.forName("com.mysql.cj.jdbc.Driver");
		Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/item", prop);
		return conn;
	}

	static void insertDatabase(Connection conn, String w1, String w2, String w3, String w4, String w5)
			throws Exception { // 寫入資料
		String sql = "INSERT INTO clothes (name,size,price,color,cnum) VALUES (?,?,?,?,?)";
		PreparedStatement pstmt = conn.prepareStatement(sql);
		pstmt.setString(1, w1);
		pstmt.setString(2, w2);
		pstmt.setString(3, w3);
		pstmt.setString(4, w4);
		pstmt.setString(5, w5);
		pstmt.executeUpdate();
	}

	static String selectDatabase(Connection conn) { // 查詢資料庫
		Statement stmt;
		StringBuilder output = new StringBuilder();
		try {
			stmt = conn.createStatement();
			String sql = "SELECT * FROM clothes";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				String name = rs.getString("name");
				String size = rs.getString("size");
				String price = rs.getString("price");
				String color = rs.getString("color");
				String cnum = rs.getString("cnum");

				output.append(name).append(",").append(size).append(",").append(price).append(",").append(color)
						.append(",").append(cnum).append("\n");
			}
		} catch (SQLException e) {
			System.out.println(e);
		}
		String result = output.toString();
		return result;
	}

	static String drawlots(Connection conn) { // 從資料庫 抽籤 可以用於廣告抽籤
		Statement stmt;
		StringBuilder output = new StringBuilder();
		try {
			stmt = conn.createStatement();
			String sql = "SELECT * FROM roll where 1=1 order by rand() limit 1";
			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {
				String luck = rs.getString("luck");
				String content = rs.getString("content");
				String details = rs.getString("details");
				output.append(luck).append("\n").append(content).append("\n").append(details);
			}
		} catch (SQLException e) {
			System.out.println(e);
		}
		String result = output.toString();
		return result;
	}

	private static String getWeatherdate() { // 查詢天氣
		String apikey = "CWB-7AE1FEA7-A66D-470C-9F9A-DDC87FE89401";
		String serchlocation = "臺中市";
		StringBuilder output = new StringBuilder();
		Map<String, Map<String, Map<String, String>>> weatherDataMap = new HashMap<>();

		try {
			URL url = new URL(String.format(
					"https://opendata.cwb.gov.tw/api/v1/rest/datastore/F-C0032-001?Authorization=%s&locationName=臺中市",
					apikey, serchlocation));
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");

			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			StringBuilder response = new StringBuilder();

			while ((line = reader.readLine()) != null) {
				response.append(line);
			}

			reader.close();
			conn.disconnect();

			// 解析回應
			JSONObject jsonResponse = new JSONObject(response.toString());
			JSONObject records = jsonResponse.getJSONObject("records");
			JSONArray locations = records.getJSONArray("location");

			// 提取天氣資訊
			for (int i = 0; i < locations.length(); i++) {
				JSONObject location = locations.getJSONObject(i);
				String locationName = location.getString("locationName");
				JSONArray weatherElements = location.getJSONArray("weatherElement");

				// 建立子Map來儲存天氣元素資訊
				Map<String, Map<String, String>> weatherElementsMap = new HashMap<>();

				// 提取各種天氣元素
				for (int j = 0; j < weatherElements.length(); j++) {
					JSONObject weatherElement = weatherElements.getJSONObject(j);
					String elementName = weatherElement.getString("elementName");
					JSONArray timeArray = weatherElement.getJSONArray("time");

					// 建立子Map來儲存時間點的天氣資訊
					Map<String, String> timeDataMap = new HashMap<>();

					// 提取各個時間點的天氣資訊
					for (int k = 0; k < timeArray.length(); k++) {
						JSONObject timeObject = timeArray.getJSONObject(k);
						String startTime = timeObject.getString("startTime");
						String endTime = timeObject.getString("endTime");
						String parameterName = timeObject.getJSONObject("parameter").getString("parameterName");

						timeDataMap.put("startTime", startTime);
						timeDataMap.put("endTime", endTime);
						timeDataMap.put("parameterName", parameterName);

					}
					weatherElementsMap.put(elementName, timeDataMap);
				}
				weatherDataMap.put(locationName, weatherElementsMap);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		// 遍歷主Map，打印地點和天氣元素資訊
		for (Map.Entry<String, Map<String, Map<String, String>>> locationEntry : weatherDataMap.entrySet()) {
			String locationName = locationEntry.getKey();
//			System.out.println("地點: " + locationName);
			output.append("地點: ").append(locationName).append("\n");

			Map<String, Map<String, String>> weatherElementsMap = locationEntry.getValue();

			// 遍歷天氣元素資訊
			for (Map.Entry<String, Map<String, String>> elementEntry : weatherElementsMap.entrySet()) {
				String elementName = elementEntry.getKey();
//				System.out.println("天氣元素: " + elementName);
				output.append("天氣元素: ").append(elementName).append("\n");

				Map<String, String> timeDataMap = elementEntry.getValue();

				// 遍歷時間點的天氣資訊
				for (Map.Entry<String, String> timeEntry : timeDataMap.entrySet()) {
					String timeKey = timeEntry.getKey();
					String timeValue = timeEntry.getValue();
//					System.out.println(timeKey + ": " + timeValue);
					output.append(timeKey).append(": ").append(timeValue).append("\n");
				}
			}

		}
		String result = output.toString();
		return result;

	}

	public static void main(String[] args) {
		new App();
	}
}
