package com.example;

import com.google.gson.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultHighlighter;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class GeminiSwing extends JFrame {
	private static final String MODEL = "gemini-2.0-flash";
	private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

	private final HttpClient http;
	private final Gson gson;
	private String apiKey;

	private final JPanel messagesPanel;
	private final JScrollPane scrollPane;
	private final JTextArea inputArea;
	private final JButton sendButton;
	private final JButton clearButton;
	private volatile boolean isSending = false;
	private Component typingIndicator;

	public GeminiSwing(String apiKey) {
		super("Gemini AI Chat");
		this.apiKey = apiKey;
		this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
		this.gson = new Gson();

		// Frame setup
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setMinimumSize(new Dimension(640, 520));
		setLayout(new BorderLayout());

		// Content background
		JPanel content = new JPanel(new BorderLayout());
		content.setBackground(new Color(0xF5, 0xF7, 0xFA));
		setContentPane(content);

		// Messages panel (vertical stream of bubbles)
		messagesPanel = new JPanel();
		messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
		messagesPanel.setBackground(new Color(0xF5, 0xF7, 0xFA));
		messagesPanel.setBorder(new EmptyBorder(12, 12, 12, 12));

		scrollPane = new JScrollPane(messagesPanel);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		add(scrollPane, BorderLayout.CENTER);

		// Bottom input area with send + clear
		JPanel bottom = new JPanel(new BorderLayout(8, 8));
		bottom.setBorder(new EmptyBorder(8, 12, 12, 12));
		bottom.setBackground(new Color(0xF5, 0xF7, 0xFA));

		inputArea = new JTextArea(3, 20);
		inputArea.setLineWrap(true);
		inputArea.setWrapStyleWord(true);
		inputArea.setFont(inputArea.getFont().deriveFont(14f));
		inputArea.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(0xD0, 0xD7, 0xDE)),
				new EmptyBorder(8, 10, 8, 10)));

		// Key bindings: Enter to send, Shift+Enter newline
		InputMap im = inputArea.getInputMap(JComponent.WHEN_FOCUSED);
		ActionMap am = inputArea.getActionMap();
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "send");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "newline");
		am.put("send", new AbstractAction() {
			@Override public void actionPerformed(ActionEvent e) { triggerSend(); }
		});
		am.put("newline", new AbstractAction() {
			@Override public void actionPerformed(ActionEvent e) { inputArea.append("\n"); }
		});

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		buttons.setOpaque(false);
		sendButton = new JButton("Send");
		sendButton.addActionListener(e -> triggerSend());
		clearButton = new JButton("Clear");
		clearButton.addActionListener(e -> clearChat());
		buttons.add(clearButton);
		buttons.add(sendButton);

		bottom.add(new JScrollPane(inputArea) {{ setBorder(BorderFactory.createEmptyBorder()); }}, BorderLayout.CENTER);
		bottom.add(buttons, BorderLayout.EAST);

		add(bottom, BorderLayout.SOUTH);

		// Menu for API key update and About
		JMenuBar menuBar = new JMenuBar();
		JMenu appMenu = new JMenu("App");
		JMenuItem setKey = new JMenuItem("Set API Key...");
		setKey.addActionListener(e -> promptForApiKey());
		JMenuItem about = new JMenuItem("About");
		about.addActionListener(e -> JOptionPane.showMessageDialog(this,
				"Gemini Swing Chat\nEnter to send, Shift+Enter for newline.",
				"About", JOptionPane.INFORMATION_MESSAGE));
		appMenu.add(setKey);
		appMenu.addSeparator();
		appMenu.add(about);
		menuBar.add(appMenu);
		setJMenuBar(menuBar);

		// Initial focus
		SwingUtilities.invokeLater(inputArea::requestFocusInWindow);

		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}

	private void triggerSend() {
		if (isSending) return;
		String msg = inputArea.getText().trim();
		if (msg.isEmpty()) return;
		appendMessage(msg, true);
		inputArea.setText("");
		setSending(true);
		showTypingIndicator();

		new Thread(() -> {
			String reply = callGemini(msg);
			SwingUtilities.invokeLater(() -> {
				hideTypingIndicator();
				appendMessage(reply, false);
				setSending(false);
			});
		}, "gemini-call").start();
	}

	private void setSending(boolean sending) {
		this.isSending = sending;
		sendButton.setEnabled(!sending);
		inputArea.setEditable(!sending);
	}

	private void clearChat() {
		messagesPanel.removeAll();
		messagesPanel.revalidate();
		messagesPanel.repaint();
	}

	private void showTypingIndicator() {
		if (typingIndicator != null) return;
		JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		row.setOpaque(false);
		JPanel bubble = createBubblePanel("AI is typing…", false, true);
		row.add(bubble);
		messagesPanel.add(row);
		messagesPanel.add(Box.createVerticalStrut(8));
		typingIndicator = row;
		scrollToBottom();
	}

	private void hideTypingIndicator() {
		if (typingIndicator == null) return;
		messagesPanel.remove(typingIndicator);
		// remove the trailing strut too
		int count = messagesPanel.getComponentCount();
		if (count > 0) {
			Component last = messagesPanel.getComponent(count - 1);
			if (last instanceof Box.Filler) {
				messagesPanel.remove(last);
			}
		}
		typingIndicator = null;
		messagesPanel.revalidate();
		messagesPanel.repaint();
	}

	private void appendMessage(String text, boolean fromUser) {
		JPanel row = new JPanel(new FlowLayout(fromUser ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
		row.setOpaque(false);
		JPanel bubble = createBubblePanel(text, fromUser, false);
		row.add(bubble);
		messagesPanel.add(row);
		messagesPanel.add(Box.createVerticalStrut(8));
		messagesPanel.revalidate();
		messagesPanel.repaint();
		scrollToBottom();
	}

	private void scrollToBottom() {
		SwingUtilities.invokeLater(() -> {
			JScrollBar bar = scrollPane.getVerticalScrollBar();
			bar.setValue(bar.getMaximum());
		});
	}

	private JPanel createBubblePanel(String text, boolean fromUser, boolean subtle) {
		Color bg = fromUser ? new Color(0xD6, 0xEC, 0xFF) : (subtle ? new Color(0xF0, 0xF2, 0xF5) : Color.WHITE);
		Color fg = Color.DARK_GRAY;
		JTextArea ta = new JTextArea(text);
		ta.setEditable(false);
		ta.setLineWrap(true);
		ta.setWrapStyleWord(true);
		ta.setOpaque(false);
		ta.setFont(ta.getFont().deriveFont(14f));
		// enable selection for copy
		ta.setHighlighter(new DefaultHighlighter());

		JPanel bubble = new JPanel(new BorderLayout());
		bubble.setBackground(bg);
		bubble.setBorder(new EmptyBorder(10, 12, 10, 12));
		bubble.add(ta, BorderLayout.CENTER);

		JPanel wrapper = new RoundedPanel(16, bg);
		wrapper.setLayout(new BorderLayout());
		wrapper.setBorder(new EmptyBorder(0, fromUser ? 48 : 0, 0, fromUser ? 0 : 48));
		wrapper.add(bubble, BorderLayout.CENTER);
		return wrapper;
	}

	private String callGemini(String userMessage) {
		try {
			JsonObject request = new JsonObject();

			JsonArray contents = new JsonArray();
			JsonObject userContent = new JsonObject();
			userContent.addProperty("role", "user");
			JsonArray parts = new JsonArray();
			JsonObject part = new JsonObject();
			part.addProperty("text", userMessage);
			parts.add(part);
			userContent.add("parts", parts);
			contents.add(userContent);

			request.add("contents", contents);

			String endpoint = String.format(BASE_URL, MODEL, apiKey);

			HttpRequest httpRequest = HttpRequest.newBuilder()
					.uri(URI.create(endpoint))
					.timeout(Duration.ofSeconds(60))
					.header("Content-Type", "application/json; charset=UTF-8")
					.POST(HttpRequest.BodyPublishers.ofString(gson.toJson(request)))
					.build();

			HttpResponse<String> resp = http.send(httpRequest, HttpResponse.BodyHandlers.ofString());

			if (resp.statusCode() / 100 != 2) {
				return "(Error: " + resp.statusCode() + ") " + resp.body();
			}

			return extractFirstText(resp.body());

		} catch (Exception e) {
			return "Request failed: " + e.getMessage();
		}
	}

	private String extractFirstText(String json) {
		try {
			JsonObject root = JsonParser.parseString(json).getAsJsonObject();
			if (!root.has("candidates")) return "(no candidates)";
			JsonArray candidates = root.getAsJsonArray("candidates");
			if (candidates.size() == 0) return "(empty)";
			JsonObject first = candidates.get(0).getAsJsonObject();
			JsonObject content = first.getAsJsonObject("content");
			JsonArray parts = content.getAsJsonArray("parts");
			return parts.get(0).getAsJsonObject().get("text").getAsString();
		} catch (Exception e) {
			return "(parse error)";
		}
	}

	private void promptForApiKey() {
		String entered = JOptionPane.showInputDialog(this, "Enter Gemini API Key:", apiKey);
		if (entered != null && !entered.isBlank()) {
			this.apiKey = entered.trim();
		}
	}

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ignored) {}

		String key = System.getenv("GEMINI_API_KEY");
		if (key == null || key.isBlank()) {
			key = JOptionPane.showInputDialog(null,
					"Enter Gemini API Key (env GEMINI_API_KEY not set):",
					"API Key Required", JOptionPane.PLAIN_MESSAGE);
			if (key == null || key.isBlank()) {
				JOptionPane.showMessageDialog(null,
						"Missing API key. Set GEMINI_API_KEY or enter it in the app menu.",
						"Error", JOptionPane.ERROR_MESSAGE);
				System.exit(1);
			}
		}
		String finalKey = key;
		SwingUtilities.invokeLater(() -> new GeminiSwing(finalKey));
	}

	// Rounded container panel for message bubbles
	private static class RoundedPanel extends JPanel {
		private final int arc;
		private final Color fill;

		RoundedPanel(int arc, Color fill) {
			this.arc = arc;
			this.fill = fill;
			setOpaque(false);
		}

		@Override protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(fill);
			g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
			g2.dispose();
		}
	}
}