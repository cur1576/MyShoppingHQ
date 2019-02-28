package com.example.myshoppinghq;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.shoppinglisthq.R;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    ShoppingMemoDataSource dataSource;
    private boolean isButtonClick = true;
    private ListView mShoppingMemosListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dataSource = new ShoppingMemoDataSource(this);

        initializeShoppingMemosListView();

        activateAddButton();
        initializeContextualActionBar();
    }

    private void initializeShoppingMemosListView() {
        List<ShoppingMemo> emptyListForInitilisation = new ArrayList<>();

        mShoppingMemosListView = findViewById(R.id.listview_shopping_memos);
        ArrayAdapter<ShoppingMemo> shoppingMemoArrayAdapter = new ArrayAdapter<ShoppingMemo>(this,
                android.R.layout.simple_list_item_multiple_choice, emptyListForInitilisation){
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position,convertView,parent);
                TextView textView = (TextView)view;

                ShoppingMemo memo = (ShoppingMemo) mShoppingMemosListView.getItemAtPosition(position);
                if(memo.isChecked()){
                    textView.setPaintFlags(textView.getPaintFlags()| Paint.STRIKE_THRU_TEXT_FLAG);
                    textView.setTextColor(Color.rgb(175,175,175));
                }else{
                    textView.setPaintFlags(textView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                    textView.setTextColor(Color.DKGRAY);
                }

                return view;
            }
        };

        mShoppingMemosListView.setAdapter(shoppingMemoArrayAdapter);

        mShoppingMemosListView.setOnItemClickListener((parent, view, position, id) -> {
            ShoppingMemo memo = (ShoppingMemo) parent.getItemAtPosition(position);
            ShoppingMemo updateMemo = dataSource.updateShoppingMemo(memo.getId(),
                    memo.getProduct(),memo.getQuantity(),!memo.isChecked());
            showAllListEntries();
        });
    }


    private void activateAddButton() {

        final EditText editTextQuantity = findViewById(R.id.editText_quantity);
        final EditText editTextProduct = findViewById(R.id.editText_product);
        Button buttonAddProduct = findViewById(R.id.button_add_prduct);
        buttonAddProduct.setOnClickListener(v -> {

            String quantityString = editTextQuantity.getText().toString();
            String product = editTextProduct.getText().toString();

            if (TextUtils.isEmpty(quantityString)) {
                editTextQuantity.setError(getString(R.string.editText_errorMessage));
                return;
            }
            if (TextUtils.isEmpty(product)) {
                editTextProduct.setError(getString(R.string.editText_errorMessage));
                return;
            }

            int quantity = Integer.parseInt(quantityString);
            editTextProduct.setText("");
            editTextQuantity.setText("");

            dataSource.createShoppingMemo(product, quantity);

            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (getCurrentFocus() != null && isButtonClick) {
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);

            }

            showAllListEntries();
        });
        editTextProduct.setOnEditorActionListener((textView, pos, keyEvent) -> {
            isButtonClick = false;
            buttonAddProduct.performClick();
            editTextQuantity.requestFocus();
            isButtonClick = true;
            return true;
        });
    }

    private void initializeContextualActionBar() {
        final ListView shoppingMemoListView = findViewById(R.id.listview_shopping_memos);
        shoppingMemoListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

        shoppingMemoListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

            int selCount = 0;

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                if (checked) {
                    selCount++;
                } else {
                    selCount--;
                }
                String cabTitel = selCount + " " + getString(R.string.cab_checked_string);
                mode.setTitle(cabTitel);
                mode.invalidate();

            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                getMenuInflater().inflate(R.menu.menu_contextual_action_bar, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                MenuItem item = menu.findItem(R.id.cab_change);
                if (selCount == 1) {
                    item.setVisible(true);
                } else {
                    item.setVisible(false);
                }
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                boolean returnValue = true;
                SparseBooleanArray touchedShoppingMemosPosition = shoppingMemoListView.getCheckedItemPositions();
                switch (item.getItemId()) {
                    case R.id.cab_delete:

                        for (int i = 0; i < touchedShoppingMemosPosition.size(); i++) {
                            boolean isChecked = touchedShoppingMemosPosition.valueAt(i);
                            if (isChecked) {
                                int positionInListView = touchedShoppingMemosPosition.keyAt(i);
                                ShoppingMemo shoppingMemo =
                                        (ShoppingMemo) shoppingMemoListView.getItemAtPosition(positionInListView);
                                Log.d(TAG, "Position im ListView: " + positionInListView + " Inhalt: " + shoppingMemo.toString());
                                dataSource.deleteShoppingMemo(shoppingMemo);
                            }
                        }
                        showAllListEntries();
                        mode.finish();
                        break;
                    case R.id.cab_change:

                        for (int i = 0; i < touchedShoppingMemosPosition.size(); i++) {
                            boolean isChecked = touchedShoppingMemosPosition.valueAt(i);
                            if (isChecked) {
                                int positionInListView = touchedShoppingMemosPosition.keyAt(i);
                                ShoppingMemo shoppingMemo =
                                        (ShoppingMemo) shoppingMemoListView.getItemAtPosition(positionInListView);
                                Log.d(TAG, "Position im ListView: " + positionInListView + " Inhalt: " + shoppingMemo.toString());
                                AlertDialog editShoppingMemoDialog = createShoppingMemoDialog(shoppingMemo);
                                editShoppingMemoDialog.show();
//                                View forKeyboard = editShoppingMemoDialog.getCurrentFocus();
//                                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
//                                imm.showSoftInput(getCurrentFocus(),InputMethodManager.RESULT_SHOWN);
                            }
                        }

                        mode.finish();
                        break;
                    default:
                        returnValue = false;
                }


                return returnValue;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                selCount = 0;
            }
        });
    }

    private AlertDialog createShoppingMemoDialog(final ShoppingMemo shoppingMemo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();

        View dialogsView = inflater.inflate(R.layout.dialog_edit_shopping_memo, null);

        final EditText editTextNewQuantity = dialogsView.findViewById(R.id.editText_new_quantity);
        editTextNewQuantity.setText(String.valueOf(shoppingMemo.getQuantity()));

        final EditText editTextNewProduct = dialogsView.findViewById(R.id.editText_new_product);
        editTextNewProduct.setText(shoppingMemo.getProduct());

        builder.setView(dialogsView)
                .setTitle(R.string.dialog_titel)
                .setPositiveButton(R.string.dialog_button_positiv, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String quantityString = editTextNewQuantity.getText().toString();
                        String product = editTextNewProduct.getText().toString();

                        if (TextUtils.isEmpty(quantityString) || TextUtils.isEmpty(product)) {
                            Toast.makeText(MainActivity.this, "Felder dürfen nicht leer sein", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        int quantity = Integer.parseInt(quantityString);

                        ShoppingMemo memo = dataSource.updateShoppingMemo(shoppingMemo.getId(), product, quantity, shoppingMemo.isChecked());

                        Log.d(TAG, "Alter Eintrag - ID: " + shoppingMemo.getId() + " Inhalt: " + shoppingMemo.toString());
                        Log.d(TAG, "Neuer Eintrag - ID: " + memo.getId() + " Inhalt: " + memo.toString());

                        showAllListEntries();
                        dialog.dismiss();
//                        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
//                        imm.hideSoftInputFromWindow(getCurrentFocus().getApplicationWindowToken(),InputMethodManager.HIDE_IMPLICIT_ONLY);
                    }
                })
                .setNegativeButton(R.string.dialog_button_negativ, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        editTextNewQuantity.setSelection(0, editTextNewQuantity.length());
        return builder.create();

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Datenquelle wird geöffnet");
        dataSource.open();
        showAllListEntries();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Datenquelle wird geschlossen.");
        dataSource.close();
    }

    private void showAllListEntries() {
        List<ShoppingMemo> shoppingMemoList = dataSource.getAllShoppingMemos();
        ArrayAdapter<ShoppingMemo> adapter = (ArrayAdapter<ShoppingMemo>) mShoppingMemosListView.getAdapter();
        adapter.clear();
        adapter.addAll(shoppingMemoList);
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Toast.makeText(this, "Settings wurde gedrückt", Toast.LENGTH_SHORT).show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void startScan(View view) {
        Intent intent = new Intent("com.google.zxing.client.android.SCAN");
        intent.putExtra("SCAN_MODE", "PRODUCT_MODE");
        try{
            startActivityForResult(intent,1);
        }catch(ActivityNotFoundException e){
            Toast.makeText(this, "Scanner nicht insatalliert", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode==1 && resultCode == RESULT_OK){
            TextView product = findViewById(R.id.editText_product);
            product.setText(getProductName(data.getStringExtra("SCAN_RESULT")));
            TextView quantity = findViewById(R.id.editText_quantity);
            quantity.requestFocus();
        }else{
            Toast.makeText(this, "Scan nicht möglich", Toast.LENGTH_SHORT).show();
        }
    }

    private String getProductName(String scanResult){
        HoleDatenTask task = new HoleDatenTask();
        String result = null;
        try {
            result = task.execute(scanResult).get();
            JSONObject rootObject = new JSONObject(result);
            Log.d(TAG, "getProductName: "+ rootObject.toString(2));
            if(rootObject.has("product")){
                JSONObject productObject = rootObject.getJSONObject("product");
                if(productObject.has("product_name")){
                    return productObject.getString("product_name");
                }
            }
        } catch (ExecutionException e) {
            Log.e(TAG, "", e);
        } catch (InterruptedException e) {
            Log.e(TAG, "", e);
        } catch (JSONException e) {
            Log.e(TAG, "", e);
        }
        return "Artikel nicht gefunden";
    }

    public class HoleDatenTask extends AsyncTask<String,Void,String> {

        @Override
        protected String doInBackground(String... strings) {
            final String baseUrl = "https://world.openfoodfacts.org/api/v0/product/";
            final String requestUrl = baseUrl + strings[0]+".json";
            // super wichtig -> um die json-url zu finden!!!
            Log.d(TAG, "doInBackground: " + requestUrl);
            StringBuilder result = new StringBuilder();
            URL url = null;

            try {
                url = new URL(requestUrl);
            } catch (MalformedURLException e) {
                Log.e(TAG, "", e);
            }
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()))) {
                String line;
                while ((line=reader.readLine())!=null){
                    result.append(line);
                }
            }catch (IOException e){

            }
            Log.d(TAG, "doInBackground: " + result.toString());
            return result.toString();
        }
    }
}
