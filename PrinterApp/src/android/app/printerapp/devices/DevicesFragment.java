package android.app.printerapp.devices;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.printerapp.ActionModeHandler;
import android.app.printerapp.R;
import android.app.printerapp.StateUtils;
import android.app.printerapp.devices.database.DatabaseController;
import android.app.printerapp.devices.discovery.JmdnsServiceListener;
import android.app.printerapp.devices.discovery.PrintNetworkManager;
import android.app.printerapp.model.ModelJob;
import android.app.printerapp.model.ModelPrinter;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.DragShadowBuilder;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TabHost.OnTabChangeListener;

/**
 * This is the fragment that will contain the Device VIEW logic
 * @author alberto-baeza
 *
 */
public class DevicesFragment extends Fragment{
	
	
	//Controllers and adapters
	private DevicesGridAdapter mGridAdapter;
	private DevicesListAdapter mListAdapter;
	
	private PrintNetworkManager mNetworkManager;
	
	//private DevicesLayoutAdapter mLayoutAdapter;
	
	//Empty constructor
	public DevicesFragment(){}

	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		/**
		 * Since API level 11, thread policy has changed and now does not allow network operation to
		 * be executed on UI thread (NetworkOnMainThreadException), so we have to add these lines to 
		 * permit it.
		 */
		if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
		
		
		
		super.onCreate(savedInstanceState);		
		//Retain instance to keep the Fragment from destroying itself
		setRetainInstance(true);
		
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		//Reference to View
		View rootView = null;
		
		//If is not new
		if (savedInstanceState==null){
			
			//Show custom option menu
			setHasOptionsMenu(true);
			
			//Inflate the fragment
			rootView = inflater.inflate(R.layout.devices_layout,
					container, false);
			
			/**
			 * CUSTOM VIEW METHODS
			 */
			
			//Set tab host for the view
			setTabHost(rootView);
						
			
			//------------------------------- View references -----------------//
									
			GridView g = (GridView) rootView.findViewById(R.id.devices_grid);

			mGridAdapter = new DevicesGridAdapter(getActivity(),
					R.layout.grid_element, DevicesListController.getList());
			
			//assign click listeners
			g.setOnItemClickListener(gridClickListener());	
			g.setOnItemLongClickListener(gridLongClickListener());
	 
			g.setAdapter(mGridAdapter);
			
			
			
			
			
			/*******************************************************************/
			
			//Reference to the first tab
			/*ViewGroup mViewGroup = (ViewGroup) rootView.findViewById(R.id.devices_grid);
			mLayoutAdapter = new DevicesLayoutAdapter(getActivity(), mViewGroup);*/
			
			
			
			//Reference to the second tab, handled by an adapter
			mListAdapter = new DevicesListAdapter(getActivity(), 
					R.layout.list_element, DevicesListController.getList());
			
			ListView l = (ListView) rootView.findViewById(R.id.devices_list);
			l.setAdapter(mListAdapter);
			
			
			
			/*************** VIDEO HANDLER ****************************/
					
			
			GridView gv = (GridView) rootView.findViewById(R.id.devices_camera);
			gv.setAdapter(new DevicesCameraAdapter(getActivity(), R.layout.video_view, DevicesListController.getList()));
			
			/*******************************************************************/
			SlidingUpPanelLayout s = (SlidingUpPanelLayout) rootView.findViewById(R.id.sliding_panel);
			TextView t = (TextView) rootView.findViewById(R.id.drag_text);
			s.setDragView(t);
			
			
			LinearLayout ll = (LinearLayout) rootView.findViewById(R.id.linearlayout_storage);
			
			new DevicesQuickprint(ll,getActivity());
			
			//Custom service listener
			new JmdnsServiceListener(this);
			mNetworkManager = new PrintNetworkManager(this);	
		
		}
		return rootView;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.devices_menu, menu);
	}
	
	//Option menu
   @Override
	public boolean onOptionsItemSelected(android.view.MenuItem item) {
	   
	   switch (item.getItemId()) {
	   
	   case R.id.menu_add: //Add a new printer
		  			
		   optionAdd();
			return true;
			
       	case R.id.menu_filter: //Filter grid / list
       		
       		optionFilter();
            return true;
              
          
       default:
           return super.onOptionsItemSelected(item);
	   }
	}
   
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	/**
	 * Constructor for the tab host
	 * TODO: Should be moved to a View class since it only handles ui.
	 */
	public void setTabHost(View v){
				 
		TabHost tabs=(TabHost) v.findViewById(android.R.id.tabhost);
		tabs.setup();
		 
		TabHost.TabSpec spec=tabs.newTabSpec("Map");
		spec.setIndicator(getString(R.string.devices_tabhost_tab_map));
		spec.setContent(R.id.tab1);
		tabs.addTab(spec);
		 
		spec=tabs.newTabSpec("List");
		spec.setIndicator(getString(R.string.devices_tabhost_tab_list));
		spec.setContent(R.id.tab2);
		tabs.addTab(spec);
		
		spec=tabs.newTabSpec("Videowall");
		spec.setIndicator(getString(R.string.devices_tabhost_tab_video));
		spec.setContent(R.id.tab3);
		tabs.addTab(spec);
		
		 
		tabs.setCurrentTab(0);
		
		tabs.setOnTabChangedListener(new OnTabChangeListener() {
		    @Override
		    public void onTabChanged(String tabId) {
		    	
		    	ActionModeHandler.modeFinish();
		        Log.i("CONTROLLER", "Tab pressed: " + tabId);
		    }
		});
		
	}
	
	public void addElement(final ModelPrinter m){
		
		getActivity().runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				if (!DatabaseController.checkExisting(m)){
					DevicesListController.addToList(m);
					m.setNotLinked();
					notifyAdapter();
				} else Log.i("DEVICES","Already exists primo");
				
			}
		});
		
	}
		
	
	
	public void notifyAdapter(){
		mListAdapter.notifyDataSetChanged();
		mGridAdapter.notifyDataSetChanged();
		
		Log.i("DEVICES","I was notified senpai!");
		
	}
	
	
	
	//Filter option for the device list
	public void optionFilter(){
		AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
		adb.setTitle(R.string.devices_filter_dialog_title);
		LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.menu_filter_dialog, null, false);
		
		final RadioGroup rg = (RadioGroup) v.findViewById(R.id.radioGroup_devices);
		
		adb.setView(v);
		
		adb.setPositiveButton(R.string.filter, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				switch(rg.getCheckedRadioButtonId()){
				
				
				case R.id.dv_radio0:{
					mGridAdapter.getFilter().filter(null);//Show all devices

				}break;
				case R.id.dv_radio1:{
					mGridAdapter.getFilter().filter("Printing");//Active printers
				}break;
				case R.id.dv_radio2:{
					mGridAdapter.getFilter().filter("Operational");	//Inactive printers
				}break;
				case R.id.dv_radio3:{
					mGridAdapter.getFilter().filter(null);	//TODO Groups
				}break;
				case R.id.dv_radio4:{
					mGridAdapter.getFilter().filter("New"); //Linked
				}break;
				
				}
				
				
			}
		});
		adb.setNegativeButton(R.string.cancel, null);
		
		adb.show();
		
	}
	
	//This is the actual discovery service
	//TODO implement the discovery logic here
	public void optionAdd(){
		
		//new DiscoveryOptionController(getActivity());
		for (ModelPrinter p : DevicesListController.getList()){
			
			p.getVideo().setZOrderOnTop(true);
			
		}
		
	}

	public void setDialogAdapter(ModelPrinter m){
		
		 AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
		 adb.setTitle(R.string.devices_progress_dialog_title);
		 adb.setIcon(getResources().getDrawable(R.drawable.printer_icon));
		 
		//Inflate the view
		LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.progress_dialog, null, false);
		
		ModelJob job = m.getJob();
		
		TextView tv1 = (TextView) v.findViewById(R.id.pd_tv1);
		tv1.setText(job.getFilename());
		
		ProgressBar pb = (ProgressBar) v.findViewById(R.id.pd_pb);
		Double n = Double.valueOf(m.getJob().getProgress() ) * 100;
		pb.setProgress(n.intValue());
		
		TextView tv2 = (TextView) v.findViewById(R.id.pd_tv2);
		tv2.setText(n.intValue() + "%");
		
		TextView tv3= (TextView) v.findViewById(R.id.pd_tv3);
		tv3.setText("Faltan " + job.getPrintTimeLeft() + " aprox");
		
		TextView tv4 = (TextView) v.findViewById(R.id.pd_tv4);
		tv4.setText(m.getTemperature());
		
		 
		adb.setView(v);
		
		adb.show();
	}
	
	public void codeDialog(final ModelPrinter m){
		
		AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
		adb.setTitle(R.string.devices_setup_title);
		
		//Inflate the view
		LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.setup_dialog, null, false);
		
		//EditText et = (EditText) v.findViewById(R.id.et_setup);

		adb.setPositiveButton(R.string.add, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {

				//m.startUpdate();
				DatabaseController.writeDb(m.getName(), m.getAddress(), String.valueOf(m.getPosition()));	
				DevicesListController.loadList(getActivity());
			}
		});
		
		adb.setNegativeButton(R.string.cancel, null);
		
		adb.setView(v);
		
		adb.show();
		
	}
	
	/******************************** click listeners *********************************/
	
	//onclick listener will open the action mode
	public OnItemClickListener gridClickListener(){

		return new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				
				ModelPrinter m = null;
				
				//search printer by position
				for (ModelPrinter mp : DevicesListController.getList()){
					if (mp.getPosition()==arg2) m = mp;
				}
				
				if (m!=null){
					
					//start action mode
					ActionModeHandler.modeStart(arg1,m);

 					 //show custom dialog
					 if (m.getStatus()== StateUtils.STATE_ERROR){
						 AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
						 adb.setTitle(R.string.devices_error_dialog_title);
						 adb.setMessage(m.getMessage());
						 adb.setIcon(getResources().getDrawable(R.drawable.warning_icon));
						 adb.show();
					 }
					 
					 if (m.getStatus()== StateUtils.STATE_PRINTING){
						setDialogAdapter(m);
					 }
					 
					 if (m.getStatus()==StateUtils.STATE_NEW){
						 codeDialog(m);
						 
						 
					 }
					 
					 if (m.getStatus()==StateUtils.STATE_ADHOC){
						 mNetworkManager.setupNetwork(DevicesFragment.this, m.getName(), m);
					 }
				} 				
			}
		};
		
	}
	
	//onlongclick will start the draggable printer handler
	public OnItemLongClickListener gridLongClickListener(){
		return new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				
				ModelPrinter m = null;
				
				for (ModelPrinter mp : DevicesListController.getList()){
					if (mp.getPosition()==arg2) m = mp;
				}
				
				if (m!=null){
					
					ClipData data = null;						
					data = ClipData.newPlainText("printer", m.getName());	
					DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(arg1);
					arg1.startDrag(data, shadowBuilder, arg1, 0);
				}
				
				
				return false;
			}
		};
	}
	
	
	
}
