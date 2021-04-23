import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.text.*;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.scene.layout.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.*;
import javafx.scene.control.Hyperlink;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.scene.control.ListView;
import javafx.geometry.*;
import javafx.scene.control.ListCell;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.ScrollPane;

import org.jsoup.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Document;
import org.jsoup.select.*;

import java.io.*;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;

public class mainPage extends Application{
	public static void main(String[] args){
		launch(args);
	}
	Scene main, results, page;
	final int width=800;
	final int height=600;
	Button search;
	TextField querry;
	Image logo;
	Background background;
	BM25 engine;
	Vector<SimpleEntry<String, Double>> resultsList;
	VBox recent;
	VBox mainLayout;
	String linkColor;
	@Override
	public void start(Stage window){
		window.setTitle("Chess Variant Search");
		try{
			logo = new Image(new FileInputStream("variantPiece.png"));
		}
		catch(Exception e){
			e.printStackTrace();
		}
		linkColor = "#A71B1B";
		refreshRecents(window);
		results = new Scene(new VBox(), window.getWidth(), window.getHeight());
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
	public void refreshRecents(Stage window){
		ArrayList<String> accessor = new ArrayList<String>();
		recent = new VBox();
		recent.setStyle("-fx-background-color: beige;");
		try(
			BufferedReader reader = new BufferedReader(new FileReader("searches.txt"));	
		){
			String line;
			while ((line = reader.readLine())!=null){
				accessor.add(line);
			}
			for (int i = accessor.size()-1;i>=0;--i){
				Hyperlink link = new Hyperlink(accessor.get(i));
				link.setStyle("-fx-text-fill: "+linkColor+";");
				final String lineC = accessor.get(i);
				link.setOnAction(e->{
					refreshResults(lineC, window);
					window.setScene(results);
				});
				TextFlow item = new TextFlow(link);
				recent.getChildren().add(item);
			}
			reader.close();
		}
		catch(Exception exep){
			exep.printStackTrace();
		}
		ScrollPane sp = new ScrollPane();
		sp.setStyle("-fx-background-color: beige;");
		sp.setContent(recent);
		sp.setPrefSize(50, 100.0);
		sp.setMaxWidth(225);
		ImageView iv = new ImageView(logo);
		iv.setFitHeight(100);
		iv.setPreserveRatio(true);
		querry = new TextField();
		querry.setMinWidth(400);
		search = new Button();
		search.setText("Search");
		search.setOnAction(e->{
			if (!querry.getText().equals("")){
				try(
					BufferedWriter writer = new BufferedWriter(new FileWriter("searches.txt", true));
				){
					writer.write(querry.getText()+"\n");
					writer.close();
				}
				catch(Exception ex){
					ex.printStackTrace();
				}
				refreshResults(querry.getText(), window);
				querry.setText("");
				window.setScene(results);
			}
		});
		BackgroundFill bf = new BackgroundFill(Color.BEIGE, CornerRadii.EMPTY , Insets.EMPTY);
		background = new Background(bf);
		HBox hb = new HBox();
		hb.setAlignment(Pos.CENTER);
		hb.getChildren().addAll(querry, search);
		HBox res = new HBox();
		Label tl = new Label("Chess Variant Search");
		Label lab = new Label("Recent Searches");
		tl.setStyle("-fx-font: 24 arial;-fx-font-weight: bold;");
		lab.setStyle("-fx-font: 18 arial;");
		res.setAlignment(Pos.CENTER);
		res.getChildren().add(recent);
		mainLayout = new VBox();
		mainLayout.setAlignment(Pos.CENTER);
		mainLayout.getChildren().addAll(iv, tl, hb, lab, sp);
		mainLayout.setBackground(background);
		main = new Scene(mainLayout, width, height);
	}
	public void refreshResults(String q, Stage window){
		// Given a querry string q, refresh results and display on results page
		engine.start(q);
		resultsList = engine.getResults();
		VBox layout = new VBox(10);
		Button back = new Button();
		back.setText("Back");
		back.setOnAction(e->{
			refreshRecents(window);
			window.setScene(main);
		});
		VBox listView = new VBox(20);
		listView.setStyle("-fx-background-color: beige;");
		listView.setPrefHeight(window.getHeight()-5);

		ArrayList<String> titleLog = new ArrayList<String>();
		for (SimpleEntry<String, Double> i : resultsList){
			String title="", snippet="", html = "";
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
				snippet = doc.select("article").text();
				if (snippet.length() > 512){
					snippet = snippet.substring(0, 512);
					snippet += "...";
				}
				for (int k = 128;k<snippet.length();k+=128){
					String newS = "";
					newS += snippet.substring(0, k);
					if (snippet.charAt(k)!=' '){
						newS += "-";
					}
					newS += "\n";
					newS += snippet.substring(k);
					snippet = newS;
				}
				input.close();
			}
			catch(Exception e){
				e.printStackTrace();
			}
			// prevent duplicate title entries
			if (!title.equals("Edit Comment")&&!title.equals("Display Item")){
				if (!titleLog.contains(title)){
					Hyperlink h = new Hyperlink(title);
					h.setStyle("-fx-text-fill: "+linkColor+";");
					final String con = html;
					h.setOnAction(e->{
						setPageContents(con, window);
						window.setScene(page);
					});
					TextFlow f = new TextFlow(h);
					Text s = new Text(snippet);
					f.setStyle("-fx-font: 24 arial;");
					VBox item = new VBox(f, s);
					listView.getChildren().add(item);
					titleLog.add(title);
				}
			}
		}
		ScrollPane sp = new ScrollPane();
		sp.setStyle("-fx-background-color: beige;");
		sp.setContent(listView);
		sp.setPrefSize(window.getHeight()-50, window.getWidth()-50);
		sp.setMaxWidth(window.getWidth()-25);
		layout.setPadding(new Insets(25, 25, 25, 25));
		layout.getChildren().addAll(back, sp);
		layout.setBackground(background);
		results = new Scene(layout, window.getWidth(), window.getHeight());
	}
	public void setPageContents(String htmlContent, Stage window){
		WebView view = new WebView();
		WebEngine wEngine = view.getEngine();
		wEngine.loadContent(htmlContent, "text/html");
		VBox cont = new VBox();
		cont.setStyle("-fx-background-color: beige;");
		Button back = new Button();
		back.setText("Back");
		back.setOnAction(e->{
			window.setScene(results);
		});
		cont.getChildren().addAll(back, view);
		page = new Scene(cont, window.getWidth(), window.getHeight());
	}
}
