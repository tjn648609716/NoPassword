package com.cwdt.junnan.nopassword_vivo;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class BaseSerializableData implements Serializable {

	private static final long serialVersionUID = 1944935881603614128L;

	public JSONObject toJsonObject() {

		JSONObject jRet = new JSONObject();
		try {
			Field[] fields = this.getClass().getFields();
			for (Field field : fields) {
				String fname = field.getName();
				Object jsObject;
				if (field.getType().isArray()) {
					JSONArray jaArray = new JSONArray();
					ArrayList<BaseSerializableData> arrayList = (ArrayList<BaseSerializableData>) field.get(this);
					for (BaseSerializableData baseSerializableData : arrayList) {
						jaArray.put(baseSerializableData.toJsonObject());
					}
					jsObject = jaArray;
				} else {
					jsObject = field.get(this);
				}
				jRet.put(fname, jsObject);
			}
		} catch (Exception e) {
			Log.e("BaseSerializableData", e.getMessage());
		}
		return jRet;
	}

	public boolean fromJson(JSONObject jsData) {
		boolean bRet = false;
		try {
			Field[] fields = this.getClass().getFields();
			for (Field field : fields) {

				// 无法支持JSON数组的转换
				if (field.getType().isArray()) {
					continue;
				}
				String fname = field.getName();
				field.setAccessible(true);
				String fvalue = jsData.optString(fname);
				if (fvalue != "") {
					String strType = field.getType().toString();
					if (strType.equals("int") || strType.equals(Integer.class.toString())) {
						field.set(this, Integer.valueOf(fvalue));
					} else {
						field.set(this, fvalue);
					}
				}
			}
			bRet = true;
		} catch (Exception e) {
			Log.e("BaseSerializableData", e.getMessage());
		}
		return bRet;
	}

	public boolean fromJsonStr(String jsonString) {
		boolean bRet = false;
		try {
			JSONObject jsdata = new JSONObject(jsonString);
			bRet = fromJson(jsdata);
		} catch (Exception e) {
			Log.e("BaseSerializableData", e.getMessage());
		}
		return bRet;
	}
}
