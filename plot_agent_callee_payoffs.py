import pandas as pd
import matplotlib.pyplot as plt
import sys

dfs = []

step_label = 'Step' 
avg_payoff = 'Avg Payoff in Window'
results_path = 'results/'

agent_types_data = ['perfect', 'selfish', 'generous']
agent_types_show = ['Pragmatic', 'Selfish', 'Considerate']

agent_payoff_labels = ["Avg Expected Callee Payoff for {} agents in Window".format(x) for x in agent_types_data]

#file_name = sys.argv[1]
ratio_string = sys.argv[1]
ratio = ratio_string.split(',')

sim_string = sys.argv[3]
#sim_tokens = sim_string.split(',')

simulation = int(sim_string)
no_of_simulations = 1

if len(sys.argv) > 2 and sys.argv[2] == '2':
    suffix = '_' + '_'.join(ratio) + '_updated_base' + '.csv'
elif len(sys.argv) > 2 and sys.argv[2] == '3':
    suffix = '_' + '_'.join(ratio) + '_multi_updated_base' + '.csv'
else:
    suffix = '_' + '_'.join(ratio) + '.csv'


#file_name = 'Results_Sim' + str(i+1) + suffix
sim_id = simulation
file_name = results_path + 'Results_Sim' + str(sim_id) + suffix
#file_name = 'Results_Sim' + str(i+1) + '.csv'
print(f'Reading file = %s' % file_name)
try:
    df = pd.read_csv(file_name)
except:
    df = None

df = df[[step_label] + agent_payoff_labels]

labels = ['Fixed', 'Sanctioning', 'Poros', 'Rule-Based RL with default', 'Rule-Based RL without explanation', 'StateRL base greedy', 'Rule-Based RL', 'LCS OG', 'LCS Sanctioning', \
        'LCS Epsilong exploration', 'LCS Initial Only Epsilon exploration', 'LCS Initial Only alternating exploration', \
        'LCS', 'LCS Butz', 'LCS without explanation', 'LCS Butz with new explanation', 'LCS with explanation', \
        'LCS Butz without explanation + own norms']
#colors = ['green', 'black', 'tab:red', 'red', 'yellow', 'purple', 'aqua', 'maroon', 'cyan', 'brown', 'pink', 'gray', 'orange', 'lightcoral', 'olive', 'coral', 'tab:blue', 'blue']

#def get_graph_name(file_name):
#    sim = get_simulation_type(file_name[11])
#    rem = file_name[13:-4]
#    ratio = rem.split('_')
#    print(f'ratio = #s' # ratio)
#    return sim + ' ' + ':'.join(ratio) + ' (Perfect:Selfish:Generous)'


#def get_simulation_type(sim):
#    if (sim == 1):
#        return 'Fixed'
#    if (sim == 2):
#        return 'Sanctioning'
#    return 'Poros'

#dfs = [x[[step_label, avg_payoff] + agent_payoff_labels] for x in dfs]

#labels = ['Fixed', 'Sanctioning', 'Poros']
colors = ['tab:green', 'tab:red', 'tab:blue', 'yellow']

## Plot the population payoffs

fig, ax = plt.subplots()
#title = get_graph_name(file_name)
ax.set(xlabel = 'Steps', ylabel = 'Social Experience')#, title = title)

for agentId in range(len(agent_types_show)):
    agent_payoff_label = agent_payoff_labels[agentId]
    ax.plot(df[step_label], df[agent_payoff_label], color = colors[agentId], label = agent_types_show[agentId])

ax.legend()
plt.show()
#import pandas as pd
#import matplotlib.pyplot as plt
#import sys
#
#dfs = []
#
#step_label = 'Step' 
#avg_payoff = 'Avg Payoff in Window'
#
#agent_types = ['perfect', 'selfish', 'generous']
#
#agent_payoff_labels = ["Avg Expected Callee Payoff for {} agents in Window".format(x) for x in agent_types]
#
#file_name = sys.argv[1]
#
#df = pd.read_csv(file_name)
#
#df = df[[step_label] + agent_payoff_labels]
#
#def get_graph_name(file_name):
#    sim = get_simulation_type(file_name[11])
#    rem = file_name[13:-4]
#    ratio = rem.split('_')
#    print(f'ratio = %s' % ratio)
#    return sim + ' ' + ':'.join(ratio) + ' (Perfect:Selfish:Generous)'
#
#
#def get_simulation_type(sim):
#    if (sim == '1'):
#        return 'Fixed'
#    if (sim == '2'):
#        return 'Sanctioning'
#    return 'Poros'
#
##dfs = [x[[step_label, avg_payoff] + agent_payoff_labels] for x in dfs]
#
##labels = ['Fixed', 'Sanctioning', 'Poros']
#colors = ['green', 'red', 'blue', 'yellow']
#
### Plot the population payoffs
#
#fig, ax = plt.subplots()
#title = get_graph_name(file_name)
#ax.set(xlabel = 'Steps', ylabel = avg_payoff, title = title)
#
#for agentId in range(len(agent_types)):
#    agent_payoff_label = agent_payoff_labels[agentId]
#    ax.plot(df[step_label], df[agent_payoff_label], color = colors[agentId], label = agent_types[agentId])
#
#ax.legend()
#plt.show()
