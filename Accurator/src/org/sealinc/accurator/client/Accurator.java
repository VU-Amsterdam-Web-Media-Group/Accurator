package org.sealinc.accurator.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.sealinc.accurator.client.component.AnnotateScreen;
import org.sealinc.accurator.client.component.ProfileScreen;
import org.sealinc.accurator.client.component.RecommendedItems;
import org.sealinc.accurator.shared.Config;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Accurator implements EntryPoint {
	interface MyUiBinder extends UiBinder<Widget, Accurator> {}

	public enum State {
		Annotate, Profile, Quality, Admin, Recommendation
	};

	@UiField
	HTMLPanel dvlogoutBlock;
	@UiField
	Panel content, header, footer;
	@UiField
	Anchor lnkRegister;
	@UiField
	Label lblLoginMessage, lblRegisterMessage, lblLoginName;
	@UiField
	Button btnDone;
	@UiField
	Anchor lnkLogout, lnkAbout, lnkLicenses;

	private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
	private List<HandlerRegistration> regs = new ArrayList<HandlerRegistration>();
	private Map<String, Double> expertise;
	private LinkedList<JsRecommendedItem> recommendedItems;

	private AnnotateScreen annotateScreen;
	private ProfileScreen profileScreen;
	private RecommendedItems recommendationScreen;
	private UserManagement management;
	private int nrInitialPrints = 50;
	private boolean hasPredefinedAnnotationOrder = false;

	private static final String castleTopic = "castle";
	private static final String floraTopic = "flora";

	private AnnotateScreen getAnnotateScreen() {
		if (annotateScreen == null) annotateScreen = new AnnotateScreen(this);
		return annotateScreen;
	}

	private ProfileScreen getProfileScreen() {
		if (profileScreen == null) profileScreen = new ProfileScreen(this);
		return profileScreen;
	}

	private RecommendedItems getRecommendationScreen() {
		if (recommendationScreen == null) recommendationScreen = new RecommendedItems(this);
		return recommendationScreen;
	}

	private UserManagement getManagement() {
		if (management == null) management = new UserManagement(this);
		return management;
	}

	@UiHandler("lnkAbout")
	void lnkAboutClick(ClickEvent e) {
		openAboutDialog();
	}

	@UiHandler("lnkLogout")
	void lnkLogoutClick(ClickEvent e) {
		// delete stored credentials
		Utility.deleteStoredUserCredentials();
		// logout the annotation component
		getManagement().logout();
		// refresh the page to show the loginpage again and reset GWT state.
		Window.Location.reload();
	}

	@UiHandler("btnDone")
	void btnDoneClick(ClickEvent e) {
		// load recommendation
		getRecommendationScreen().loadNextRecommendations();
		History.newItem(State.Recommendation.toString());
	}

	@UiHandler("lnkRegister")
	void registerClickHandler(ClickEvent e) {
		getManagement().closeLogin();
		getManagement().openRegister();
	}

	public void onModuleLoad() {
		RootPanel rootPanel = RootPanel.get();
		Widget w = uiBinder.createAndBindUi(this);
		rootPanel.add(w);
		initHistorySupport();
		loadUIThemeElements();

		dvlogoutBlock.setVisible(false);
		btnDone.setVisible(false);

		resizeContent();
		Window.addResizeHandler(new ResizeHandler() {

			Timer resizeTimer = new Timer() {
				@Override
				public void run() {
					resizeContent();
				}
			};

			@Override
			public void onResize(ResizeEvent event) {
				resizeTimer.cancel();
				resizeTimer.schedule(250);
			}
		});

		getManagement().login();
	}

	public native void showLoading(boolean show) /*-{
		if (show)
			$wnd.jQuery(".loading").show();
		else
			$wnd.jQuery(".loading").hide();
	}-*/;

	protected native void openAboutDialog() /*-{
		$wnd.jQuery("#dialog-about").dialog({
			autoOpen : false,
			modal : true,
			draggable : false,
			resizable : false,
		});
		$wnd.jQuery("#dialog-about").dialog("open");
	}-*/;

	private native void loadUIThemeElements()/*-{
		$wnd.jQuery("button").button();
		$wnd.jQuery(".button").button();
		$wnd.jQuery("#menu").menu({
			position : {
				my : "right top",
				at : "right+5 top+25"
			}
		});
	}-*/;

	public void updateLanguageForAnnotationComponent() {
		RequestCallback callback = new RequestCallback() {

			@Override
			public void onResponseReceived(Request request, Response response) {
				JsArray<JsUserProfileEntry> entries = Utility.parseUserProfileEntry(response.getText());
				if (entries != null && entries.length() > 0) {
					String language = entries.get(0).getValueAsString();
					getAnnotateScreen().setLanguage(language);
				}
				else
				{
					//default to dutch
					getAnnotateScreen().setLanguage("nl");
				}
			}

			@Override
			public void onError(Request request, Throwable exception) {
				exception.printStackTrace();
			}
		};
		Utility.getUserProfileEntry(Utility.getQualifiedUsername(), "languagePreference", null, Utility.getQualifiedUsername(), callback);
	}

	protected void resizeContent() {
		int headerHeight = header.getOffsetHeight();
		int footerHeight = footer.getOffsetHeight();
		int windowHeight = Window.getClientHeight();
		int height = windowHeight - headerHeight - footerHeight;
		String mainContentHeight = height + "px";
		content.setHeight(mainContentHeight);
	}

	protected void loadRecommendations() {
		String url = Config.assignComponentURL + "?" + "strategy=" + Config.assignComponentStrategy + "&nritems="
				+ Config.assignComponentNrItems + "&user=" + Utility.getQualifiedUsername();

		// show the loader image if we are on the recommendation screen
		if (State.Recommendation.toString().equals(History.getToken())) showLoading(true);

		Utility.adminService.getJSON(url, new AsyncCallback<String>() {

			@Override
			public void onSuccess(String json) {
				// if json == null, retry
				if (json == null) {
					loadRecommendations();
				}
				else {
					recommendedItems = new LinkedList<JsRecommendedItem>();
					JsArray<JsRecommendedItem> recs = parseRecommendations(json);
					for (int i = 0; i < recs.length(); i++) {
						recommendedItems.add(recs.get(i));
					}
					getRecommendationScreen().loadNextRecommendations();
					if (State.Recommendation.toString().equals(History.getToken())) showLoading(false);
				}
			}

			@Override
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
				// try again
				loadRecommendations();
			}
		});
	}

	/**
	 * Reloads the complete page with the new language. All GWT state is lost.
	 * 
	 * @param language
	 */
	public void changeLanguage(String language) {
		// store the new preference
		Utility.storeUserProfileEntry(Utility.getQualifiedUsername(), "languagePreference", null, Utility.getQualifiedUsername(), language,
				"string");
		// load Accurator with the new locale
		String newURL = Window.Location.createUrlBuilder().setParameter(LocaleInfo.getLocaleQueryParam(), language).buildString();
		Window.Location.replace(newURL);
	}

	public void userPropertyChanged(String dimension) {
		if ("expertise".equals(dimension)) {
			loadRecommendations();
		}
	}

	private void annotate(String resourceURI, String topic) {
		// extra stylesheet for annotation component
		String stylesheet = Window.Location.getProtocol() + "//" + Window.Location.getHost() + "/css/jacconator.css";
		String url = Config.annotationComponentURL + "?target=" + resourceURI + "&stylesheet=" + stylesheet;
		String ui = "";
		if (floraTopic.equals(topic)) {
			ui = "&ui=http://semanticweb.cs.vu.nl/annotate/nicheAccuratorFlowerDemoUi";
		}
		else if (castleTopic.equals(topic)) {
			ui = "&ui=http://semanticweb.cs.vu.nl/annotate/nicheAccuratorCastleDemoUi";
		}
		// complete url for the iframe containing the annotation component
		url += ui;
		getAnnotateScreen().loadResource(resourceURI, url);

	}

	public void annotate(final String resourceURI) {
		// determine the topic of the resource
		String topic = Utility.getResourceTopic(resourceURI);
		// if not known, get it and store it
		if (topic == null) {
			// get the new one
			Utility.itemService.getTopic(resourceURI, new AsyncCallback<String>() {

				@Override
				public void onSuccess(String topic) {
					Utility.setResourceTopic(resourceURI, topic);
					annotate(resourceURI, topic);
				}

				@Override
				public void onFailure(Throwable caught) {}
			});
		}
		else {
			annotate(resourceURI, topic);
		}
	}

	private final native JsArray<JsUserProfileEntry> parseExpertise(String json) /*-{
		return eval(json);
	}-*/;

	private final native JsArray<JsRecommendedItem> parseRecommendations(String json) /*-{
		return eval(json);
	}-*/;

	public void updateExpertise(String topic, double value) {
		if (expertise != null) expertise.put(topic, value);
	}

	protected void loadExpertise() {
		RequestCallback callback = new RequestCallback() {
			@Override
			public void onResponseReceived(Request request, Response response) {
				String json = response.getText();
				JsArray<JsUserProfileEntry> entries = parseExpertise(json);
				expertise = new HashMap<String, Double>();
				for (int i = 0; i < entries.length(); i++) {
					JsUserProfileEntry entry = entries.get(i);
					double value = entry.getValueAsDouble();
					expertise.put(entry.getScope(), value);
				}
			}

			@Override
			public void onError(Request request, Throwable exception) {}
		};
		// get all expertises
		Utility.getUserProfileEntry(Utility.getQualifiedUsername(), "expertise", null, null, callback);
	}

	protected void loadFirstPrintForAnnotation() {
		// if we did not yet load the flowers and castles, wait and try again
		if (recommendedItems == null || expertise == null) {
			Timer t = new Timer() {
				@Override
				public void run() {
					loadFirstPrintForAnnotation();
				}
			};
			t.schedule(400);
		}
		else {
			// we have the prints and expertise
			List<String> uris = getNextPrintsToAnnotate(1);
			if (uris.size() > 0) {
				String uri = uris.get(0);
				// manually add the first print seen the to the seen list in the
				// recommendation screen
				getRecommendationScreen().addFirstSeenPrint(uri);
				annotate(uri);
			}
			else System.err.println("Could not annotate next print. No print available. Waiting and trying again...");
		}

	}

	/**
	 * Assumes that all data has been loaded (prints and expertise). If not it
	 * return an empty list
	 * 
	 * @param nrPrints
	 * @return
	 */
	public List<String> getNextPrintsToAnnotate(int nrPrints) {
		List<String> uris = new ArrayList<String>();
		if (expertise == null) {
			return uris;
		}
		// based on UP and recommendation
		if (recommendedItems == null || recommendedItems.size() == 0) {
			return uris;
		}
		for (int i = 0; i < nrPrints; i++) {
			JsRecommendedItem rec = recommendedItems.removeFirst();
			Utility.setResourceTopic(rec.getURI(), rec.getScope());
			uris.add(rec.getURI());
		}
		return uris;
	}

	private void LoadState(String token) {
		State state = null;
		try {
			try {
				state = State.valueOf(token);
			}
			catch (IllegalArgumentException ex) {
				state = State.Annotate;
			}
			btnDone.setVisible(false);
			content.clear();

			switch (state) {

				case Annotate:
					btnDone.setVisible(true);
					content.add(getAnnotateScreen());
					break;
				case Profile:
					content.add(getProfileScreen());
					getProfileScreen().loadUIThemeElements();
					getProfileScreen().loadData();
					break;
				case Quality:
					break;
				case Admin:
					break;
				case Recommendation:
					content.add(getRecommendationScreen());
					break;
			}
			loadUIThemeElements();
		}
		catch (Exception ex) {
			ex.printStackTrace();
			// state not parsable
		}
	}

	protected void loadCurrentHistory() {
		String token = History.getToken();
		if (token == null || token.isEmpty()) {
			History.newItem(State.Recommendation.toString());
		}
		else{
			LoadState(token);
		}
	}

	public static native void trackGoogleAnalytics(String historyToken) /*-{
		try {

			// setup tracking object with account
			var pageTracker = $wnd._gat._getTracker("UA-37706543-1");

			pageTracker._setRemoteServerMode();

			// turn on anchor observing
			pageTracker._setAllowAnchor(true)

			// send event to google server
			pageTracker._trackPageview(historyToken);

		} catch (err) {

			// debug
			alert('FAILURE: to send in event to google analytics: ' + err);
		}
	}-*/;

	private void initHistorySupport() {
		History.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				String token = event.getValue();
				LoadState(token);
				trackGoogleAnalytics(token);
			}
		});
	}
}
