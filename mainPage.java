import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.text.*;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.control.Hyperlink;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.scene.control.ListView;

import org.jsoup.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Document;
import org.jsoup.select.*;

import java.io.File;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;

public class mainPage extends Application{
	Scene main, results, page;
	final int width=800;
	final int height=600;
	Button search;
	TextField querry;
	BM25 engine;
	Vector<SimpleEntry<String, Double>> resultsList;
	@Override
	public void start(Stage window){
		window.setTitle("Chess Variant Search");
		querry = new TextField();
		search = new Button();
		search.setText("Search");
		search.setOnAction(e->{
			if (!querry.getText().equals("")){
				refreshResults(querry.getText(), window);
				querry.setText("");
				window.setScene(results);
			}
		});
		VBox vb = new VBox(20, querry, search);
		main = new Scene(vb, width, height);
		VBox layout = new VBox(20);
		results = new Scene(layout, window.getWidth(), window.getHeight());
		window.setScene(main);
		window.show();
		engine = new BM25();
		page = new Scene(new VBox(), window.getWidth(), window.getHeight());	
	}
	@Override
	public void stop(){
		// preserve reverse index
		engine.close();
	}
	public void refreshResults(String q, Stage window){
		// Given a querry string q, refresh results and display on results page
		engine.start(q);
		resultsList = engine.getResults();
		VBox layout = new VBox(10);
		Button back = new Button();
		back.setText("Back");
		back.setOnAction(e->{
			window.setScene(main);
		});
		ListView<TextFlow> listView = new ListView<TextFlow>();
		listView.setPrefHeight(window.getHeight()-100);
		for (SimpleEntry<String, Double> i : resultsList){
			String title="", html = "";
			try{
				File source = new File(i.getKey());
				Document doc = Jsoup.parse(source, "UTF-8");
				Scanner input = new Scanner(source);
				while (input.hasNext()){
					String line = input.nextLine();
					html += line;
				}
				title = doc.title();
				if (title.length() > 512){
					title = title.substring(0, 512);
					title += "...";
				}
				input.close();
			}
			catch(Exception e){
				e.printStackTrace();
			}
			Hyperlink h = new Hyperlink(title);
			final String con = html;
			h.setOnAction(e->{
				setPageContents(con, window);
				window.setScene(page);
			});
			TextFlow f = new TextFlow(h);
			listView.getItems().add(f);
		}
		layout.getChildren().addAll(back, listView);
		results = new Scene(layout, window.getWidth(), window.getHeight());
	}
	public void setPageContents(String htmlContent, Stage window){
		WebView view = new WebView();
		WebEngine wEngine = view.getEngine();
		wEngine.loadContent(htmlContent, "text/html");
		VBox cont = new VBox();
		Button back = new Button();
		back.setText("Back");
		back.setOnAction(e->{
			window.setScene(results);
		});
		cont.getChildren().addAll(back, view);
		page = new Scene(cont, window.getWidth(), window.getHeight());
	}
}
