package tv.memoryleakdeath.ascalondreams.state;

public class StateMachine {
   private GameState currentGameState;
   private static StateMachine stateMachine;

   private StateMachine() {
      currentGameState = GameState.RUNNING;
   }

   public static StateMachine getInstance() {
      if(stateMachine == null) {
         stateMachine = new StateMachine();
      }
      return stateMachine;
   }

   public synchronized GameState getCurrentGameState() {
      return currentGameState;
   }

   public synchronized void setCurrentGameState(GameState currentGameState) {
      this.currentGameState = currentGameState;
   }
}
