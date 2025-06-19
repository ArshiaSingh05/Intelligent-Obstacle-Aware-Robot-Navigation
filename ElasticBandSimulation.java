// ElasticBandSimulation.java
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.stage.Stage;
import java.util.*;

public class ElasticBandSimulation extends Application {

    /* -------- constants -------- */
    final int WIDTH = 800, HEIGHT = 600;     /* pixels */
    final double defaultRobotRadius    = 8;  /* velocity in pixels per frame */
    final double defaultObstacleRadius = 15;

    /* -------- state -------- */
    Point goal   = new Point(WIDTH - 50, 50);
    Point robot  = new Point(50, HEIGHT - 50);

    List<Point>  obstacles               = new ArrayList<>();
    List<Point>  obsDir                  = new ArrayList<>();
    List<Double> obstacleRadii           = new ArrayList<>();
    List<Double> obstacleSpeedMultipliers= new ArrayList<>();

    int obstacleCount = 5;          // default obstacle number
    double globalObstacleSpeedMultiplier = 1.0;

    boolean isPlacingObstacles = false;
    boolean isPlacingStart     = false;
    boolean isPlacingGoal      = false;
    int     obstacleIndex      = 0;

    Random rand = new Random();

    /* -------- JavaFX handles -------- */
    AnimationTimer animation;
    boolean isRunning = false;
    Canvas   canvas;
    GraphicsContext gc;

    /* -------- simple point helper -------- */
    class Point { double x, y; Point(double x,double y){this.x=x;this.y=y;} }

    /* ====================================================================== */
    @Override public void start(Stage stage) {
        /* canvas */
        canvas = new Canvas(WIDTH, HEIGHT);
        gc     = canvas.getGraphicsContext2D();
        initializeObstacles();

        /* ---------- mouse click logic ------------------------------------ */
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {

            /*  placing START */
            if (isPlacingStart) {
                robot.x = e.getX(); robot.y = e.getY();
                isPlacingStart = false; drawFrame(); return;
            }
            /*  placing GOAL */
            if (isPlacingGoal) {
                goal.x  = e.getX();  goal.y  = e.getY();
                isPlacingGoal = false; drawFrame(); return;
            }
            /*  placing OBSTACLES */
            if (isPlacingObstacles) {
                if (obstacleIndex < obstacleCount) {
                    obstacles.get(obstacleIndex).x = e.getX();
                    obstacles.get(obstacleIndex).y = e.getY();
                    obstacleIndex++;
                    if (obstacleIndex == obstacleCount) {
                        isPlacingObstacles = false;
                        showAlert("All obstacles placed!");
                    }
                    drawFrame();
                }
                return;
            }

            /*  clicking existing obstacle to edit */
            for (int i = 0; i < obstacles.size(); i++) {
                Point o = obstacles.get(i);
                double r = obstacleRadii.get(i);
                if (Math.hypot(e.getX()-o.x, e.getY()-o.y) <= r + 5) {
                    showObstacleEditDialog(i);
                    return;
                }
            }

            /*  plain click – just set goal */
            goal.x = e.getX(); goal.y = e.getY();  drawFrame();
        });

        /* ---------- controls --------------------------------------------- */
        Button startBtn          = new Button("Start");
        Button pauseBtn          = new Button("Pause");
        Button resetBtn          = new Button("Reset");
        Button setObsBtn         = new Button("Set Obstacles");
        Button placeObstaclesBtn = new Button("Place Obstacles");
        Button setStartBtn       = new Button("Set Start Position");
        Button setGoalBtn        = new Button("Set Goal Position");

        Label obsLabel  = new Label("Number of Obstacles Required");
        TextField obsField = new TextField(String.valueOf(obstacleCount));
        obsField.setMaxWidth(100);

        Label speedLabel = new Label("Obstacle Speed:");
        speedLabel.setStyle("-fx-font-weight:bold; -fx-font-size:13px;");
        VBox.setMargin(speedLabel, new Insets(25,0,0,0));

        Slider speedSlider = new Slider(0,10,1);
        speedSlider.setShowTickLabels(true);
        speedSlider.setShowTickMarks(true);
        speedSlider.setMajorTickUnit(2);
        speedSlider.setMinorTickCount(1);
        speedSlider.valueProperty().addListener((o,ov,nv)->globalObstacleSpeedMultiplier=nv.doubleValue());

        /* button handler */
        startBtn.setOnAction(e->{ if(!isRunning){ animation.start(); isRunning=true; } });
        pauseBtn.setOnAction(e->{ animation.stop(); isRunning=false; });
        resetBtn.setOnAction(e->{
            animation.stop(); isRunning=false;
            robot=new Point(50,HEIGHT-50);
            initializeObstacles(); drawFrame();
        });

        setObsBtn.setOnAction(e->{
            try{
                int count=Integer.parseInt(obsField.getText().trim());
                if(count<=0||count>100){ showAlert("Enter 1‑100"); return;}
                obstacleCount=count; initializeObstacles(); drawFrame();
            }catch(NumberFormatException ex){ showAlert("Invalid number"); }
        });

        placeObstaclesBtn.setOnAction(e->{
            isPlacingObstacles=true; isPlacingStart=isPlacingGoal=false;
            obstacleIndex=0; initializeObstacles(true); drawFrame();
        });

        setStartBtn.setOnAction(e->{
            isPlacingStart=true; isPlacingGoal=isPlacingObstacles=false;
        });
        setGoalBtn.setOnAction(e->{
            isPlacingGoal=true;  isPlacingStart=isPlacingObstacles=false;
        });

        String baseStyle="""
          -fx-background-color: linear-gradient(to right,#00b09b,#96c93d);
          -fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:14px;
          -fx-background-radius:12px; -fx-border-radius:12px;
          -fx-padding:10 20 10 20; -fx-cursor:hand;
          -fx-effect:dropshadow(three-pass-box,rgba(0,0,0,0.3),5,0,0,1);
        """;
        for(Button b: List.of(startBtn,pauseBtn,resetBtn,setObsBtn,
                              placeObstaclesBtn,setStartBtn,setGoalBtn)){
            b.setStyle(baseStyle);
            b.setOnMouseEntered(e->b.setStyle(baseStyle+"-fx-scale-x:1.05;-fx-scale-y:1.05;"));
            b.setOnMouseExited (e->b.setStyle(baseStyle));
            b.setMinWidth(160);
        }

        VBox controls=new VBox(15,startBtn,pauseBtn,resetBtn,
                obsLabel,obsField,setObsBtn,placeObstaclesBtn,
                setStartBtn,setGoalBtn,
                speedLabel,speedSlider);
        controls.setPadding(new Insets(20));
        controls.setStyle("-fx-background-color:linear-gradient(to bottom,#f9f9f9,#dddddd);");
        controls.setPrefWidth(200);

        HBox root = new HBox(controls, canvas);
        Scene scene=new Scene(root);

        /* animation loop */
        animation=new AnimationTimer(){
            public void handle(long now){ updateObstacles(); moveRobot(); drawFrame();}
        };

        stage.setScene(scene);
        stage.setTitle("Elastic Band Robot Simulation");
        stage.show();

        drawFrame();
    }

    
    private void initializeObstacles(){ initializeObstacles(false);}
    private void initializeObstacles(boolean manual){
        obstacles.clear(); obsDir.clear(); obstacleRadii.clear(); obstacleSpeedMultipliers.clear();
        for(int i=0;i<obstacleCount;i++){
            obstacles.add( manual? new Point(-100,-100)
                                 : new Point(100+rand.nextDouble()*400,100+rand.nextDouble()*300) );
            obsDir.add(new Point((rand.nextDouble()-0.5)*3,(rand.nextDouble()-0.5)*3));
            obstacleRadii.add(defaultObstacleRadius);
            obstacleSpeedMultipliers.add(1.0);
        }
    }

    /* ---------------- obstacles update ---------------- */
    private void updateObstacles(){
        if(isPlacingObstacles) return;
        for(int i=0;i<obstacles.size();i++){
            Point p=obstacles.get(i), d=obsDir.get(i);
            double speed=globalObstacleSpeedMultiplier*obstacleSpeedMultipliers.get(i);
            p.x+=d.x*speed; p.y+=d.y*speed;
            if(p.x<0||p.x>WIDTH) d.x*=-1;
            if(p.y<0||p.y>HEIGHT) d.y*=-1;
        }
    }

    /* ---------------- robot move ---------------- */
    private void moveRobot(){
        double ax=goal.x-robot.x, ay=goal.y-robot.y;
        double dist=Math.hypot(ax,ay);
        if(dist<5) return;

        ax/=dist; ay/=dist;

        double rx=0, ry=0;
        for(int i=0;i<obstacles.size();i++){
            Point o=obstacles.get(i);
            double radius=obstacleRadii.get(i);
            double dx=robot.x-o.x, dy=robot.y-o.y;
            double d =Math.hypot(dx,dy);
            if(d<radius+80){
                double strength=Math.min(1000/(d*d+1e-3),5.0);
                rx+=strength*(dx/(d+1e-6));
                ry+=strength*(dy/(d+1e-6));
            }
        }

        double vx=ax+rx, vy=ay+ry;
        double norm=Math.hypot(vx,vy)+1e-6;

        double speed=2.5+0.5*globalObstacleSpeedMultiplier;
        double step=Math.min(speed,6.0);

        robot.x+=step*vx/norm;
        robot.y+=step*vy/norm;
    }

    /* ---------------- drawing ---------------- */
    private void drawFrame(){
        /* background */
        gc.setFill(new LinearGradient(0,0,1,1,true, CycleMethod.NO_CYCLE,
                new Stop(0,Color.web("#0f2027")), new Stop(1,Color.web("#2c5364"))));
        gc.fillRect(0,0,WIDTH,HEIGHT);

        /* grid */
        gc.setStroke(Color.rgb(255,255,255,0.05)); gc.setLineWidth(1);
        for(int x=0;x<WIDTH;x+=40) gc.strokeLine(x,0,x,HEIGHT);
        for(int y=0;y<HEIGHT;y+=40) gc.strokeLine(0,y,WIDTH,y);

        /* border */
        gc.setStroke(Color.rgb(0,255,255,0.3)); gc.setLineWidth(4);
        gc.strokeRect(0,0,WIDTH,HEIGHT);

        /* goal */
        double pulse=1.0+0.15*Math.sin(System.nanoTime()/2.5e8);
        double gRad=defaultRobotRadius*1.6*pulse;
        RadialGradient goalGrad=new RadialGradient(0,0,goal.x,goal.y,gRad,false,
                CycleMethod.NO_CYCLE,new Stop(0,Color.rgb(255,70,70)),new Stop(1,Color.DARKRED));
        gc.setFill(goalGrad); gc.fillOval(goal.x-gRad,goal.y-gRad,gRad*2,gRad*2);
        gc.setStroke(Color.BLACK); gc.setLineWidth(1);
        gc.strokeOval(goal.x-gRad,goal.y-gRad,gRad*2,gRad*2);

        /* obstacles */
        for(int i=0;i<obstacles.size();i++){
            Point o=obstacles.get(i); double r=obstacleRadii.get(i);
            RadialGradient green=new RadialGradient(0,0,o.x,o.y,r,false,CycleMethod.NO_CYCLE,
                    new Stop(0,Color.LIGHTGREEN), new Stop(1,Color.DARKGREEN));
            gc.setFill(green); gc.fillOval(o.x-r,o.y-r,r*2,r*2);
            gc.setStroke(Color.rgb(0,0,0,0.6)); gc.setLineWidth(0.7);
            gc.strokeOval(o.x-r,o.y-r,r*2,r*2);
        }

        /* robot */
        gc.setEffect(new DropShadow(6,Color.rgb(0,0,0,0.3)));
        gc.setFill(Color.LIGHTBLUE); gc.fillRoundRect(robot.x-8,robot.y-26,16,14,5,5);
        gc.setFill(Color.WHITE); gc.fillOval(robot.x-5.5,robot.y-23,3,3); gc.fillOval(robot.x+2.5,robot.y-23,3,3);
        gc.setStroke(Color.DARKBLUE); gc.setLineWidth(1);
        gc.strokeLine(robot.x-4,robot.y-18,robot.x+4,robot.y-18);
        gc.setStroke(Color.GRAY); gc.strokeLine(robot.x,robot.y-26,robot.x,robot.y-32);
        gc.setFill(Color.RED); gc.fillOval(robot.x-1.5,robot.y-34,3,3);
        gc.setFill(Color.SLATEGRAY); gc.fillRoundRect(robot.x-9,robot.y-12,18,22,6,6);
        gc.setFill(Color.DARKGRAY); gc.fillRect(robot.x-5,robot.y-5,10,10);
        gc.setStroke(Color.DIMGRAY); gc.setLineWidth(3);
        gc.strokeLine(robot.x-9,robot.y-8, robot.x-16,robot.y+4);
        gc.strokeLine(robot.x+9,robot.y-8, robot.x+16,robot.y+4);
        gc.strokeLine(robot.x-4,robot.y+10,robot.x-4,robot.y+18);
        gc.strokeLine(robot.x+4,robot.y+10,robot.x+4,robot.y+18);
        gc.setStroke(Color.BLACK); gc.setLineWidth(0.5);
        gc.strokeRoundRect(robot.x-9,robot.y-12,18,22,6,6);
        gc.strokeRoundRect(robot.x-8,robot.y-26,16,14,5,5);
    }

    /* ---------------- obstacle editing dialog ---------------- */
    private void showObstacleEditDialog(int idx){
        Dialog<Void> dlg=new Dialog<>(); dlg.setTitle("Edit Obstacle "+(idx+1));
        Label sizeLbl=new Label("Radius:");
        Slider sizeSl=new Slider(5,50, obstacleRadii.get(idx));
        sizeSl.setShowTickMarks(true); sizeSl.setShowTickLabels(true);

        Label spLbl= new Label("Speed Multiplier:");
        Slider spSl= new Slider(0,5, obstacleSpeedMultipliers.get(idx));
        spSl.setShowTickMarks(true); spSl.setShowTickLabels(true);

        VBox box=new VBox(15,sizeLbl,sizeSl,spLbl,spSl); box.setPadding(new Insets(20));
        dlg.getDialogPane().setContent(box);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK);
        dlg.setResultConverter(btn->{
            obstacleRadii.set(idx,sizeSl.getValue());
            obstacleSpeedMultipliers.set(idx,spSl.getValue()); return null;
        });
        dlg.showAndWait(); drawFrame();
    }

    private void showAlert(String msg){
        Alert a=new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    public static void main(String[] args){ 
        launch(); 
    }
}
