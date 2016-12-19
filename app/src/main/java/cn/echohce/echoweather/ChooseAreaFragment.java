package cn.echohce.echoweather;


import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.echohce.echoweather.db.City;
import cn.echohce.echoweather.db.County;
import cn.echohce.echoweather.db.Province;
import cn.echohce.echoweather.util.HttpUtil;
import cn.echohce.echoweather.util.Utility;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by lin on 2016/12/16.
 */

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;

    public static final int LEVEL_CITY = 1;

    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog mProgressDialog;

    private List<Province> mProvinceList;

    private List<City> mCityList;

    private List<County> mCountyList;

    private City selectedCity;

    private Province selectedProvince;

    private int currentLevel;

    private ArrayAdapter<String> mAdapter;

    private List<String> dataList = new ArrayList<>();

    TextView mTitleText;

    Button mBackButton;

    ListView mChooseListView;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);

        mTitleText = (TextView) view.findViewById(R.id.title_text);
        mBackButton = (Button) view.findViewById(R.id.back_button);
        mChooseListView = (ListView) view.findViewById(R.id.list_view);

        mAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1,dataList);
        mChooseListView.setAdapter(mAdapter);


        return view;
    }



    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLevel == LEVEL_COUNTY) queryCities();
                else if (currentLevel == LEVEL_CITY) queryProvinces();
            }
        });

        mChooseListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE){
                    selectedProvince = mProvinceList.get(position);
                    queryCities();
                }
                else if (currentLevel == LEVEL_CITY){
                    selectedCity = mCityList.get(position);
                    queryCounties();
                }
                else if (currentLevel == LEVEL_COUNTY){
                    String weatherId = mCountyList.get(position).getWeatherId();
                    Intent intent = new Intent(getActivity(), WeatherActivity.class);
                    intent.putExtra("weather_id", weatherId);
                    startActivity(intent);
                    getActivity().finish();
                }
            }
        });
        queryProvinces();
    }

    private void queryProvinces() {
        mTitleText.setText("中国");
        mBackButton.setVisibility(View.GONE);
        mProvinceList = DataSupport.findAll(Province.class);
        if (mProvinceList.size() > 0) {
            dataList.clear();
            for (Province p : mProvinceList)
            {
                dataList.add(p.getProvinceName());
            }
            mAdapter.notifyDataSetChanged();
            mChooseListView.smoothScrollToPosition(0);
            currentLevel = LEVEL_PROVINCE;
        } else {
            String address = "http://guolin.tech/api/china";
            queryFromServer(address, "province");
        }
    }

    private void queryCities() {
        mTitleText.setText(selectedProvince.getProvinceName());
        mBackButton.setVisibility(View.VISIBLE);
        mCityList = DataSupport.where("provinceid = ?", String.valueOf(selectedProvince.getId())).find(City.class);
        if (mCityList.size() > 0) {
            dataList.clear();
            for (City city : mCityList) {
                dataList.add(city.getCityName());
            }

            mAdapter.notifyDataSetChanged();
            mChooseListView.smoothScrollToPosition(0);
            currentLevel = LEVEL_CITY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address, "city");
        }
    }

    private void queryCounties() {
        mTitleText.setText(selectedCity.getCityName());
        mBackButton.setVisibility(View.VISIBLE);
        mCountyList = DataSupport.where("cityid = ?", String.valueOf(selectedCity.getId())).find(County.class);
        if (mCountyList.size() > 0) {
            dataList.clear();
            for (County c : mCountyList)
                dataList.add(c.getCountyName());
            mAdapter.notifyDataSetChanged();
            mChooseListView.smoothScrollToPosition(0);
            currentLevel = LEVEL_COUNTY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = String.format("http://guolin.tech/api/china/%d/%d", provinceCode, cityCode);
            queryFromServer(address, "county");
        }
    }

    private void queryFromServer(String address, final String type) {
        showProgressDialog();
        HttpUtil.sendOkhttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                Log.d("Hello",responseText);
                boolean result = false;
                if ("province".equals(type))
                    result = Utility.handlerProvinceResponse(responseText);
                else if ("city".equals(type))
                    result = Utility.handlerCityResponse(responseText, selectedProvince.getId());
                else if ("county".equals(type))
                    result = Utility.handlerCountyResponse(responseText, selectedCity.getId());

                if (result) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(type))
                                queryProvinces();
                            else if ("city".equals(type))
                                queryCities();
                            else if ("county".equals(type))
                                queryCounties();
                        }
                    });
                }

            }
        });
    }


    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setMessage("正在加载...");
            mProgressDialog.setCanceledOnTouchOutside(false);
        }
        mProgressDialog.show();
    }

    private void closeProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }
}
