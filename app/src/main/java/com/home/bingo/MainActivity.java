package com.home.bingo;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements FirebaseAuth.AuthStateListener, View.OnClickListener {

    private static final String TAG = MainActivity.class.getCanonicalName();
    private static final int RC_SING_IN = 100;
    private FirebaseAuth auth;
    private TextView nickText;
    private ImageView avatar;
    private Group groupAvatars;
    int[] avatarIds = {R.drawable.avatar_0,
            R.drawable.avatar_1,
            R.drawable.avatar_2,
            R.drawable.avatar_3,
            R.drawable.avatar_4,
            R.drawable.avatar_5,
            R.drawable.avatar_6};
    private Member member;
    private FirebaseRecyclerAdapter<GameRoom, RoomHolder> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        auth = FirebaseAuth.getInstance();
        findView();

    }

    private void findView() {
        nickText = findViewById(R.id.nickname);
        avatar = findViewById(R.id.avatar);
        nickText.setOnClickListener(view ->{
            showNickDialog(nickText.getText().toString());
        } );


       /* nickText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNickDialog(nickText.getText().toString());
            }
        });*/
        groupAvatars = findViewById(R.id.group_avatars);
        groupAvatars.setVisibility(View.GONE);
        avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               groupAvatars.setVisibility(
                       groupAvatars.getVisibility() == View.GONE ? View.VISIBLE : View.GONE
               );
            }
        });
        findViewById(R.id.avatar_0).setOnClickListener(this);
        findViewById(R.id.avatar_1).setOnClickListener(this);
        findViewById(R.id.avatar_2).setOnClickListener(this);
        findViewById(R.id.avatar_3).setOnClickListener(this);
        findViewById(R.id.avatar_4).setOnClickListener(this);
        findViewById(R.id.avatar_5).setOnClickListener(this);
        findViewById(R.id.avatar_6).setOnClickListener(this);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText roomEdit =  new EditText(MainActivity.this);
                roomEdit.setText("Welcome");
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Game Room")
                        .setMessage("Please enter room title")
                        .setView(roomEdit)
                        .setNeutralButton("Cancel",null)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String roomTitle = roomEdit.getText().toString();
                                GameRoom room  = new GameRoom(roomTitle,member);
                                FirebaseDatabase.getInstance().getReference("rooms")
                                        .push().setValue(room, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(@Nullable DatabaseError error,
                                                           @NonNull DatabaseReference ref) {
                                        if(error == null){
                                            String roomId = ref.getKey();
                                            FirebaseDatabase.getInstance().getReference("rooms")
                                                    .child(roomId)
                                                    .child("id")
                                                    .setValue(roomId);
                                            Intent bingo = new Intent(MainActivity.this,BingoActivity.class);
                                            bingo.putExtra("ROOM_ID",roomId);
                                            bingo.putExtra("IS_CREATOR",true);
                                            startActivity(bingo);
                                        }
                                    }
                                });


                            }
                        }).show();
            }
        });

        //RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        Query query = FirebaseDatabase.getInstance()
                .getReference("rooms")
                .limitToLast(30);
        FirebaseRecyclerOptions<GameRoom> options =
                new FirebaseRecyclerOptions.Builder<GameRoom>()
                        .setQuery(query,GameRoom.class)
                .build();
        adapter = new FirebaseRecyclerAdapter<GameRoom, RoomHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull RoomHolder holder, int position, @NonNull GameRoom model) {
                holder.imageView.setImageResource(avatarIds[model.init.avatarId]);
                holder.titleText.setText(model.title);
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent bingo = new Intent(MainActivity.this,BingoActivity.class);
                        bingo.putExtra("ROOM_ID",model.id);
                        startActivity(bingo);
                    }
                });
            }

            @NonNull
            @Override
            public RoomHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.row_room,parent,false);
                return  new RoomHolder(view);
            }
        };
        recyclerView.setAdapter(adapter);

    }
    public class RoomHolder extends RecyclerView.ViewHolder{
        ImageView imageView;
        TextView  titleText;
        public RoomHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.room_image);
            titleText =  itemView.findViewById(R.id.room_title);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        auth.addAuthStateListener(this);
        adapter.startListening();

    }

    @Override
    protected void onStop() {
        super.onStop();
        auth.removeAuthStateListener(this);
        adapter.stopListening();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_menu_signout:
                FirebaseAuth.getInstance().signOut();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth auth) {
        FirebaseUser user = auth.getCurrentUser();
        if ( user == null) {
            startActivityForResult(
                    AuthUI.getInstance().createSignInIntentBuilder()
                    .setAvailableProviders(Arrays.asList(
                            new AuthUI.IdpConfig.EmailBuilder().build(),
                            new AuthUI.IdpConfig.GoogleBuilder().build()
                    ))
                            .setIsSmartLockEnabled(false)
                            .build()
                    , RC_SING_IN);
        } else {
            Log.d(TAG, "onAuthStateChanged: " + user.getEmail() + "/"+user.getUid());
            String displayName = user.getDisplayName();
            FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(user.getUid())
                    .child("displayName")
                    .setValue(user.getDisplayName());
            FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(user.getUid())
                    .child("uid")
                    .setValue(user.getUid());


            FirebaseDatabase.getInstance().getReference("users")
                    .child(user.getUid())
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            member = snapshot.getValue(Member.class);
                            if (member != null) {
                                if(member.nickname !=null){
                                    nickText.setText(member.nickname);
                                }else{
                                    showNickDialog(displayName);
                                }
                            }


                            avatar.setImageResource(avatarIds[member.avatarId]);

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });


            /*FirebaseDatabase.getInstance().
                    getReference("users")
                    .child(user.getUid())
                    .child("nickname")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(snapshot.getValue() != null){
                                String nickname = (String) snapshot.getValue();
                            }else {
                                showNickDialog(displayName);

                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });*/

        }


    }

    private void showNickDialog(String displayName) {
        EditText nickEdit =  new EditText(this);
        nickEdit.setText(displayName);
        new AlertDialog.Builder(this)
                .setTitle("Your nickname")
                .setMessage("Please enter your nickname")
                .setView(nickEdit)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String nickname = nickEdit.getText().toString();
                        FirebaseDatabase.getInstance().
                                getReference("users")
                                .child(auth.getUid())
                                .child("nickname")
                                .setValue(nickname);

                    }
                }).show();
    }

    @Override
    public void onClick(View view) {
        int selectedId = 0;
        if (view instanceof ImageView) {
            switch (view.getId()){
                case R.id.avatar_0:
                    selectedId = 0;
                    break;
                case R.id.avatar_1:
                    selectedId = 1;
                    break;
                case R.id.avatar_2:
                    selectedId = 2;
                    break;
                case R.id.avatar_3:
                    selectedId = 3;
                    break;
                case R.id.avatar_4:
                    selectedId = 4;
                    break;
                case R.id.avatar_5:
                    selectedId = 5;
                    break;
                case R.id.avatar_6:
                    selectedId = 6;
                    break;
            }

            groupAvatars.setVisibility(View.GONE);
            FirebaseDatabase.getInstance().getReference("users")
                    .child(auth.getUid())
                    .child("avatarId")
                    .setValue(selectedId);


        }
    }
}