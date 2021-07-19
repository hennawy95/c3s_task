# c3s_task

# Bully Algorithm

###  Description
Multiple instances of the application should run and communicate with each other


#### When New Process Run
1.Try to communicate with the coordinator
  1.If no response then the process is set to be the coordinator
  2.If there is response then coordinatore respond with existing process to the new process
2.Coordinator send Alive message every 1 sec.
3.If the coordinator didn't send alive then the process who dedect this send election message to every process with process id greater than it self
  1.If the process didn't get response it set it self to be the coordinatore and send Victory message to all process. 

