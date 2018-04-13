package com.example.olegh.swizio;


import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.Manifest;
import android.os.Environment;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.POIXMLException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends Activity {

    //Initialize file inputs, buttons, listviews and charts
    File file;
    Button back,graph_;
    ArrayList<String> path;
    String lastDirectory;
    int count = 0;
    ListView storage;
    LineChart LineChart;
    ArrayList<Entry> entries = new ArrayList<>();
    ArrayList<String> labels = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {



        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //request user permissions to access memory
        permissions();

        //Find views, create ArrayLists, start graphs
        storage = findViewById(R.id.storage);
        back = findViewById(R.id.back);
        LineChart = findViewById(R.id.LineChart);
        count = 0;
        path = new ArrayList<>();
        path.add(count,System.getenv("EXTERNAL_STORAGE"));
        storage();
        graph_=findViewById(R.id.graph);

        graph_.setOnClickListener(new View.OnClickListener() {@Override public void onClick(View v) {setData(entries);
        }});



        //Initiate the reading of excel file or directory movement
        storage.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                lastDirectory = path.get(count);
                if(lastDirectory.equals(adapterView.getItemAtPosition(i))){
                    read_data(lastDirectory);
                }
                else{
                    count++;
                    path.add(count,(String) adapterView.getItemAtPosition(i));
                    storage();
                }
            }
        });


        //Moves directory up for the "Back" button
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(count == 0){
                    toast("Highest level of directory");
                }else{
                    path.remove(count);
                    count--;
                    storage();
                }
            }
        });
    }



    //Toast for replying to user actions
    public void toast(String text){
        Context context = getApplicationContext();
        Toast toast = Toast.makeText(context,text, Toast.LENGTH_SHORT);
        toast.show();
    }



    //Check permission or request a new one
    private void permissions() {
        int permissionCheck = this.checkSelfPermission("Manifest.permission.READ_EXTERNAL_STORAGE");
        permissionCheck += this.checkSelfPermission("Manifest.permission.WRITE_EXTERNAL_STORAGE");
        if (permissionCheck != 0) {
            this.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE}, 1001); //Any number
        }
    }



    //Check for internal storage and create a new file with a list of paths
    private void storage() {
        try{
            if (!Environment.getExternalStorageState().equals(
                    Environment.MEDIA_MOUNTED)) {
                toast("No SD card found.");
            }
            else{
                // Locate the image folder in your SD Car;d
                file = new File(path.get(count));
            }

            File[] listFile = file.listFiles();
            String[] filePathStrings = new String[listFile.length];

            for (int i = 0; i < listFile.length; i++) {
                // Get the path of the image file
                filePathStrings[i] = listFile[i].getAbsolutePath();

            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filePathStrings);
            storage.setAdapter(adapter);
        }
        catch(NullPointerException e){
            toast("Double click the file to upload");
        }
    }



    //Identify excel rows and columns, and stores them in StringBuilder
    private void read_data(String filePath) {
        File inputFile = new File(filePath);

        try {
            InputStream inputStream = new FileInputStream(inputFile);
            XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = workbook.getSheetAt(0);
            int rc = sheet.getPhysicalNumberOfRows();
            FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
            StringBuilder strings = new StringBuilder();

            for (int r = 1; r < rc; r++) {
                Row row = sheet.getRow(r);
                int cellsCount = row.getPhysicalNumberOfCells();
                for (int n = 0; n < cellsCount; n++) {
                    if(n>2){
                        toast("ERROR: Excel file incorrectly formatted");
                        break;
                    }
                    else{
                        String value = get_string(row, n, formulaEvaluator);
                        strings.append(value).append(", ");
                    }
                }
                strings.append(">");
            }
            toast("Upload complete");
            fillarray(strings);


        }catch (FileNotFoundException e) {
            toast("File not found");

        } catch (IOException e) {
            toast("Error with data stream");
        }
        catch (POIXMLException e) {
            toast("Wrong file type");
        }

    }



    //Converts excel data types into strings
    private String get_string(Row row, int c, FormulaEvaluator formulaEvaluator) {
        String s = "";
        try {
            Cell i = row.getCell(c);
            CellValue cellValue = formulaEvaluator.evaluate(i);
            switch (cellValue.getCellType()) {
                case Cell.CELL_TYPE_NUMERIC:
                    double numericValue = cellValue.getNumberValue();
                    if(HSSFDateUtil.isCellDateFormatted(i)) {
                        double date = cellValue.getNumberValue();
                        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter =
                                new SimpleDateFormat("MM/dd-HH:mm");
                        s = formatter.format(HSSFDateUtil.getJavaDate(date));
                    } else {
                        s = ""+numericValue;
                    }
                    break;
                case Cell.CELL_TYPE_STRING:
                    s = ""+cellValue.getStringValue();
                    break;
                case Cell.CELL_TYPE_BOOLEAN:
                    s = ""+cellValue.getBooleanValue();
                    break;
            }
        }
        catch (NullPointerException e) {
        }
        return s;
    }






    //get strings from "read_data" and add them to the "upload" ArrayList
    public void fillarray(StringBuilder mStringBuilder) {

        String[] rows = mStringBuilder.toString().split(">");
        int i = 0;
        for (String row : rows) {
            String[] columns = row.split(",");
            try {

                i = i+1;
                entries.add(new Entry( i, Float.parseFloat(columns[1])));
                labels.add(columns[0]);

            } catch (NumberFormatException e) {
                toast("Missing values in the excel file");

            }
        }
    }




    //Set ArraysLists and graph the data
    public void setData(ArrayList<Entry> entries) {

            LineDataSet dataSet = new LineDataSet(entries, "Label");
            LineData data = new LineData(dataSet);
            dataSet.setColors(ColorTemplate.MATERIAL_COLORS[3]);
            dataSet.setCircleColor(ColorTemplate.MATERIAL_COLORS[3]);
            dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
            YAxis yright = LineChart.getAxisRight();
            yright.setEnabled(false);


            XAxis xAxis = LineChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setLabelRotationAngle(45);

            xAxis.setValueFormatter(new IAxisValueFormatter() {
                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    return MainActivity.this.labels.get((int)value);
                }
            });
            LineChart.setData(data);
            LineChart.getDescription().setEnabled(false);
            LineChart.setTouchEnabled(true);
            LineChart.setDragEnabled(true);
            LineChart.setScaleEnabled(true);
            LineChart.invalidate();
    }
}
