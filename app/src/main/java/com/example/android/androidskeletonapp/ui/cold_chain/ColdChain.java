package com.example.android.androidskeletonapp.ui.cold_chain;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.android.androidskeletonapp.R;
import com.example.android.androidskeletonapp.data.Sdk;

import org.hisp.dhis.android.core.arch.call.D2Progress;
import org.hisp.dhis.android.core.enrollment.Enrollment;
import org.hisp.dhis.android.core.event.Event;
import org.hisp.dhis.android.core.event.EventCreateProjection;
import org.hisp.dhis.android.core.event.EventStatus;
import org.hisp.dhis.android.core.maintenance.D2Error;
import org.hisp.dhis.android.core.program.ProgramStageDataElement;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityDataValue;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import io.reactivex.Observable;

public class ColdChain extends AppCompatActivity {


    private Object JSONArray;

    public static Intent getIntent(Context context){
        return new Intent(context, ColdChain.class);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cold_chain);
        Toolbar toolbar = findViewById(R.id.coldChainToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setContentView(R.layout.content_cold_chain);
        TextView teival = findViewById(R.id.teiInfo);
        TextView tempval = findViewById(R.id.temperatureData);
        TextView createdval = findViewById(R.id.created);
        View addTemp = findViewById(R.id.addBtn);


        Observable<D2Progress> downloadData = downloadTrackedEntityInstances();
        System.out.println("download data : " + downloadData);
        Observable<D2Progress> downloadEvents = downloadSingleEvents();
        System.out.println("download events : " + downloadEvents);

        //Denne iden funker - jeg får events på FGsKhDsTBEU
        String testTeiUid = "FGsKhDsTBEU";
        String enrollmentId = "6";

        List<String> teiUids = getTeiUids();

        for(int i = 0; i < teiUids.size(); i++){
            System.out.println("TEIUIDS :: " + teiUids.get(i));
        }
        List<Enrollment> enrollmentID = getEnrollmentIds(testTeiUid);
        for(int i = 0; i < enrollmentID.size(); i++){
            System.out.println("EnrollmentUIDS :: " + enrollmentID.get(i));
        }
        List<Event> eventList = getEvents(enrollmentId);

        System.out.println("Eventlist ::-- " + eventList);

        List<TrackedEntityDataValue> eventListValues = getEventsTeiData(testTeiUid);

        System.out.println(eventListValues);

        for (int i = 0; i < eventListValues.size() ; i++) {
            System.out.println("EventsValues: " + eventListValues.get(i));
        }


        //setter temperatur verdien til string
        teival.setText("TeiUID : " + testTeiUid + "\n EnrollemntID : " + enrollmentId);
        tempval.setText("Temperature: " + getTeiTempValue(testTeiUid));
        createdval.setText("Created: " + getTeiCreatedValue(testTeiUid));

        addEvent("g5oklCs7xIg","SDuMzcGLh8i","aecqgkE5quA", "iMDPax84iAN");
        JSONArray jsonData = getJsonArray();
        for (int i = 0; i < jsonData.length(); i++) {
            try {
                System.out.println(jsonData.get(i));
                JSONObject c = jsonData.getJSONObject(i);
                String temp = c.getString("temperature");
                String date = c.getString("Date");
                System.out.println(temp);
                System.out.println(date);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    // Creats a new event on this date foreach element in json file, fetches it's metadata dataElementUid and sets it value to temperature.
    public boolean addEvent (String enrollmentID, String programUid , String programStageId, String ouUid) {
        String defaultOptionCombo = Sdk.d2().categoryModule().categoryOptionCombos()
                .byDisplayName().eq("default").one().blockingGet().uid();
        try {
            JSONArray jsonData = getJsonArray();
            for (int i = 0; i < jsonData.length(); i++) {
                try {
                    System.out.println(jsonData.get(i));
                    JSONObject c = jsonData.getJSONObject(i);
                    String temp = c.getString("temperature");
                    String date = c.getString("Date");
                    System.out.println(temp);
                    System.out.println(date);

                    //makes a new event and returns it eventUID
                    String eventUid = Sdk.d2().eventModule().events().blockingAdd(
                            EventCreateProjection.builder()
                                    .enrollment(enrollmentID)
                                    .program(programUid)
                                    .programStage(programStageId)
                                    .organisationUnit(ouUid)
                                    .attributeOptionCombo(defaultOptionCombo)
                                    .build()
                    );

                    //sets the created-, enrollment- and event-date to this date.
                    System.out.println("Ny Event UID :::::  " + eventUid);
                    Sdk.d2().eventModule().events().uid(eventUid).setEventDate(new Date(date));
                    Sdk.d2().enrollmentModule().enrollments().uid("g5oklCs7xIg").setEnrollmentDate(new Date(date));
                    Sdk.d2().eventModule().events().uid(eventUid).setCompletedDate(new Date(date));

                    //Get's the event's dataElement UID and set's a value to the event
                    List<ProgramStageDataElement> programDataElem = Sdk.d2().programModule().programStageDataElements().blockingGet();
                    for (ProgramStageDataElement elem: programDataElem
                    ) {
                        Sdk.d2().trackedEntityModule().trackedEntityDataValues().value(eventUid, elem.dataElement().uid()).blockingSet(temp);
                    }

                    //set eventStatus and upload
                    Sdk.d2().eventModule().events().uid(eventUid).setStatus(EventStatus.COMPLETED);
                    Sdk.d2().eventModule().events().upload();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return true;
        } catch (D2Error d2Error) {
            d2Error.printStackTrace();
            return false;
        }
    }

    public JSONArray getJsonArray(){
        try {
            return new JSONArray(getJson());
        }catch (JSONException | IOException e ){
            e.printStackTrace();
        }
        return null;
    }

    //Metoden fyller inn textview med json-data og viser en toast ved onClick.
    public void addJsonToView(View v) {
        TextView jsonData = findViewById(R.id.tempJson);
        try {
            jsonData.setText(getJson());

        } catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText(ColdChain.this, "Data lagt til",
                Toast.LENGTH_LONG).show();
    }

    // Leser json filen som man finner i Res/raw og returnerer json-objektene som stringer.
    private String getJson() throws IOException {
        InputStream is = getResources().openRawResource(R.raw.temp_small);
        Writer writer = new StringWriter();
        char[] buffer = new char[1024];
        try {
            Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            is.close();
        }

        return writer.toString();
    }

    // Henter eventdata på en tei og kegger det til i en liste, itererer over lista og returnerer value som er temperaturen til den tei-en.
    private String getTeiTempValue(String testTeiUid){
        List<TrackedEntityDataValue> teiAttributeValues= getEventsTeiData(testTeiUid);

        for (int i = 0; i < teiAttributeValues.size() ; i++) {
            System.out.println("Temp-data: " + teiAttributeValues.get(i).value());
            return teiAttributeValues.get(i).value();
        }
        return null;
    }

    // Itererer over Tei sine events og returnerer veriden når eventen er opprettet i string format.
    private String getTeiCreatedValue(String testTeiUid){
        List<TrackedEntityDataValue> teiAttributeValues= getEventsTeiData(testTeiUid);

        for (int i = 0; i < teiAttributeValues.size() ; i++) {
            System.out.println("Created: " + teiAttributeValues.get(i).created());
            return teiAttributeValues.get(i).created().toString();
        }
        return null;
    }

    //Returnerer en tei sine datavalues fra en event.
    private List<TrackedEntityDataValue> getEventsTeiData(String testTeiUid){
        List<Event> eventDir= getEventsDirectly(testTeiUid);
        for (int i = 0; i < eventDir.size() ; i++) {
            return eventDir.get(i).trackedEntityDataValues();
        }
        return null;
    }

    // henter events direkte ved å bruke teiuid
    private List<Event> getEventsDirectly(String testTeiUid){
        return Sdk.d2().eventModule().events().byTrackedEntityInstanceUids(Collections.singletonList(testTeiUid)).withTrackedEntityDataValues().blockingGet();
    }

    // Kan også hente events på denne måten
    private List<Event> getEvents(String testEnrollmentId){
        return  Sdk.d2().eventModule().events().byEnrollmentUid().eq(testEnrollmentId).withTrackedEntityDataValues().blockingGet();
    }

    // Henter ut ennrolmentid til ein tei
    private List<Enrollment> getEnrollmentIds(String testTeiUid){
        return Sdk.d2().enrollmentModule().enrollments().byTrackedEntityInstance().eq(testTeiUid).blockingGet();
    }

    // Returnerer alle UID til TEI i et program
    private List<String> getTeiUids(){
        return Sdk.d2().trackedEntityModule().trackedEntityInstances().blockingGetUids();
    }

    // Downloads tei's
    private Observable<D2Progress> downloadTrackedEntityInstances() {
        return Sdk.d2().trackedEntityModule().trackedEntityInstanceDownloader()
                .limit(10).limitByOrgunit(false).limitByProgram(false).download();
    }
    // Downloads events
    private Observable<D2Progress> downloadSingleEvents() {
        return Sdk.d2().eventModule().eventDownloader()
                .limit(10).limitByOrgunit(false).limitByProgram(false).download();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

}
