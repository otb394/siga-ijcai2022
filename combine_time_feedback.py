import pandas as pd
import matplotlib.pyplot as plt
import sys

dfs = []

step_label = 'Step' 
avg_time = 'Avg time for feedbacks in Window'
results_path = 'results/'

no_of_simulations = 4

ratio_string = sys.argv[1]
ratio = ratio_string.split(',')
suffix = '_' + '_'.join(ratio) + '.csv'

for i in range(no_of_simulations):
    file_name = results_path + 'Results_Sim' + str(i+1) + suffix
    #file_name = 'Results_Sim' + str(i+1) + '.csv'
    print(f'Reading file = %s' % file_name)
    try:
        dfs.append(pd.read_csv(file_name))
    except:
        dfs.append(None)

for df in dfs:
    if avg_time not in df:
        df[avg_time] = 0

dfs = [x[[step_label, avg_time]] for x in dfs if x is not None]

labels = ['Fixed', 'Sanctioning', 'Poros', 'Only RL']
colors = ['green', 'black', 'blue', 'red']

title = 'Average Payoffs for ratio ' + ':'.join(ratio) +  ' (Perfect:Selfish:Generous)'

fig, ax = plt.subplots()
ax.set(xlabel = 'Steps', ylabel = avg_time, title = title)

for i in range(no_of_simulations):
    df = dfs[i]
    if (df is not None):
        ax.plot(df[step_label], df[avg_time], color = colors[i], label = labels[i])

plt.legend()
plt.show()
