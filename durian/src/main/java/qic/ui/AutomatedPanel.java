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
package qic.ui;

import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import qic.Command;
import qic.Main;
import qic.util.Config;
import qic.util.Dialogs;
import qic.util.SoundUtils;
import qic.util.Util;

/**
 * @author thirdy
 *
 */
public class AutomatedPanel extends JPanel {
	private static final String AUTOMATED_TXT_FILENAME = "automated.txt";

	private static final long serialVersionUID = 1L;
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	
	JLabel statusLbl = new JLabel("Status");
	JButton runBtn = new JButton("Force");

	private Main main;
	private JTextArea searchListTa = new JTextArea(10, 15);
	private SearchResultTable table = new SearchResultTable();
	
	private int waitMins = Integer.parseInt(Config.getPropety(Config.AUTOMATED_SEARCH_WAIT_MINUTES, "10"));
	private int UPDATE_FREQ = waitMins * 60 * 1000;
	private ActionListener runCommand = e -> (new QueryTask(this)).execute();
	private javax.swing.Timer timer = new javax.swing.Timer(0, runCommand);
	
	public AutomatedPanel(Main main) {
		super(new BorderLayout(5, 5));
		this.main = main;
		
		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.X_AXIS));
		controlPanel.add(statusLbl);
		controlPanel.add(runBtn);
		
		List<String> searchList = Util.loadSearchList(AUTOMATED_TXT_FILENAME);
		searchListTa.setText(searchList
				.stream()
				.collect(joining(lineSeparator())));

		runBtn.addActionListener(runCommand);

		JPanel eastPanel = new JPanel(new BorderLayout(5, 5));
		eastPanel.add(controlPanel, BorderLayout.NORTH);
		eastPanel.add(searchListTa, BorderLayout.CENTER);

		this.add(new JScrollPane(table), BorderLayout.CENTER);
		this.add(eastPanel, BorderLayout.EAST);
		
		timer.setDelay(UPDATE_FREQ);
		logger.info("Starting timer...");
		timer.restart();    
	}
	

    private static class QueryTask extends SwingWorker<Void, Command> {
    	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    	AutomatedPanel panel;
    	
        public QueryTask(AutomatedPanel panel) {
			this.panel = panel;
		}

		@Override
        protected Void doInBackground() {
			panel.runBtn.setEnabled(false);
			panel.table.clear();
			String text = panel.searchListTa.getText();
			if (!text.isEmpty()) {
				String[] searches = text.split("\n");
				logger.info("searches: " + Arrays.toString(searches));
				int total = 0;
	            int idx = 0;
	            while (!isCancelled() && idx < searches.length) {
	            	String prefix = Config.getPropety(Config.AUTOMATED_SEARCH_PREFIX, "tmpsc online bo").trim();
					String search = searches[idx].trim();
					String line = String.format("s %s %s", prefix, search);
	            	logger.info("Now running search: " + line);
					Command command = runQuery(line);
	                idx++;
	                total += command.itemResults.size();
	                publish(command);
	            }
	            if (total > 0) {
	            	try {
						SoundUtils.tone(5000,100);
					} catch (LineUnavailableException e) {
						e.printStackTrace();
					}
				}
			}
			panel.runBtn.setEnabled(true);
            return null;
        }

        @Override
        protected void process(List<Command> command) {
        	for (Command cmd : command) {
        		panel.table.addData(cmd.itemResults);
			}
        }
        
    	private Command runQuery(String line) {
    		int count = 0;
    		int maxTries = 10;
    		while(true) {
    		    try {
    		    	return panel.main.processLine(line);
    		    } catch (IOException e) {
    		        if (++count == maxTries) throw new RuntimeException(e);
    		    }
    		}
    	}
    }
    
	public void saveToFile() {
		try {
			Util.overwriteFile(AUTOMATED_TXT_FILENAME, searchListTa.getText());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
