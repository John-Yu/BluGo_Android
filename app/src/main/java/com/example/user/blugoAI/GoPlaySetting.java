package com.example.user.blugoAI;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by user on 2016-06-09.
 */
public class GoPlaySetting implements Parcelable {
    public int rule = GoRule.RuleID.JAPANESE.getValue(); /* 0:japna, 1:china */
    public float komi = 6.5f;
    public int size = 19;
    public int wb = 0; /* 0: Random, 1: black, 2: white */
    public int handicap = 0; /* 0 ~ 25, only for 19x19 */

    public GoPlaySetting() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(rule);
        dest.writeFloat(komi);
        dest.writeInt(size);
        dest.writeInt(wb);
        dest.writeInt(handicap);
    }

    // this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
    public final static Parcelable.Creator<GoPlaySetting> CREATOR = new Parcelable.Creator<GoPlaySetting>() {
        public GoPlaySetting createFromParcel(Parcel in) {
            return new GoPlaySetting(in);
        }

        public GoPlaySetting[] newArray(int size) {
            return new GoPlaySetting[size];
        }
    };

    // example constructor that takes a Parcel and gives you an object populated with it's values
    private GoPlaySetting(Parcel in) {
        rule = in.readInt();
        komi = in.readFloat();
        size = in.readInt();
        wb = in.readInt();
        handicap = in.readInt();
    }
}
