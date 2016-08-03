package luft27.usbterminal;

import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by amalikov on 03.08.2016.
 */
public class TerminalEmulator {
	public TerminalEmulator(TextView ui) {
		this.ui = ui;
		lines = new ArrayList<>();
		currentLine = new StringBuilder();
		escSequence = new StringBuilder();
	}

	public void put(String str) {
		for (char s : str.toCharArray()) {
			if (!processEscSequence(s)) {
				processText(s);
			}
		}

		redraw();
	}

	private void processText(char s) {
		switch (s) {
		case '\r':
			break;
		case '\n':
			lines.add(currentLine.toString());
			currentLine.setLength(0);
			break;
		default:
			currentLine.append(s);
		}
	}

	private boolean processEscSequence(char s) {
		if (escSequence.length() == 0 && s != 27) {
			return false;
		}

		if (s == 27) {
			escSequence.setLength(0);
		}

		escSequence.append(s);

		if (s != '[' && s >= 64 && s <= 126) {
			// end of esc sequence
			escSequence.setLength(0);
		}

		return true;
	}

	private void redraw() {
		StringBuilder sb = new StringBuilder();

		for (String line : lines) {
			sb.append(line);
			sb.append("\n");
		}

		sb.append(currentLine.toString());

		output(sb.toString());
	}

	private void output(String text) {
		ui.setText(text);

		final int scrollAmount = ui.getLayout().getLineTop(ui.getLineCount()) - ui.getHeight();

		if (scrollAmount > 0) {
			ui.scrollTo(0, scrollAmount);
		} else {
			ui.scrollTo(0, 0);
		}
	}

	private final TextView ui;
	private final List<String> lines;
	private final StringBuilder currentLine;
	private final StringBuilder escSequence;
}
