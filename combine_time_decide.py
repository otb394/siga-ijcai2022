import pandas as pd
import matplotlib.pyplot as plt
import sys

dfs = []

step_label = 'Step' 
avg_time = 'Avg time to decide in Window'
results_path = 'results/'


#no_of_simulations = 6
simulations = []

ratio_string = sys.argv[1]
ratio = ratio_string.split(',')

sim_string = sys.argv[3]
sim_tokens = sim_string.split(',')
if len(sim_tokens) == 1:
    simulations = range(1, int(sim_tokens[0]) + 1)
else:
    simulations = [int(x) for x in sim_tokens]
no_of_simulations = len(simulations)

if len(sys.argv) > 2 and sys.argv[2] == '2':
    suffix = '_' + '_'.join(ratio) + '_updated_base' + '.csv'
elif: len(sys.argv) > 2 and sys.argv[2] == '3':
    suffix = '_' + '_'.join(ratio) + '_multi_updated_base' + '.csv'
else:
    suffix = '_' + '_'.join(ratio) + '.csv'

for i in range(no_of_simulations):
    #file_name = 'Results_Sim' + str(i+1) + suffix
    sim_id = simulations[i]
    file_name = results_path + 'Results_Sim' + str(sim_id) + suffix
    #file_name = 'Results_Sim' + str(i+1) + '.csv'
    print(f'Reading file = %s' % file_name)
    try:
        dfs.append(pd.read_csv(file_name))
    except:
        dfs.append(None)

for df in dfs:
    if df is not None and avg_time not in df:
        df[avg_time] = 0

dfs = [x[[step_label, avg_time]] if x is not None else None for x in dfs]

labels = ['Fixed', 'Sanctioning', 'Poros', 'Rule-Based RL with default', 'Rule-Based RL without explanation', 'StateRL base greedy', 'Rule-Based RL', 'LCS OG', 'LCS Sanctioning', \
        'LCS Epsilong exploration', 'LCS Initial Only Epsilon exploration', 'LCS Initial Only alternating exploration', \
        'LCS', 'LCS Butz', 'LCS Butz without explanation', 'LCS Butz with new explanation', 'LCS Butz with new explanation + own context', \
        'LCS Butz without explanation + own norms']
colors = ['green', 'black', 'blue', 'red', 'yellow', 'purple', 'aqua', 'maroon', 'cyan', 'brown', 'pink', 'gray', 'orange', 'lightcoral', 'olive', 'coral', 'tab:blue', 'tab:red']

title = 'Average time to decide for ratio ' + ':'.join(ratio) +  ' (Perfect:Selfish:Generous)'


fig, ax = plt.subplots()
ax.set(xlabel = 'Steps', ylabel = avg_time, title = title)

for i in range(no_of_simulations):
    df = dfs[i]
    sim_id = simulations[i]
    if (df is not None):
        ax.plot(df[step_label], df[avg_time], color = colors[sim_id - 1], label = labels[sim_id - 1])

plt.legend()
plt.show()
