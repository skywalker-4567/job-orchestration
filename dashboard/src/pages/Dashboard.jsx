import { useEffect, useState } from 'react';
import { getJobMetrics, getExecutionMetrics } from '../services/api';
import JobList from '../components/JobList';

function MetricCard({ label, value, accent }) {
  return (
    <div style={{
      background: '#1e1e2e',
      border: `1px solid ${accent ? accent + '55' : '#313244'}`,
      borderRadius: '8px',
      padding: '20px 24px',
      minWidth: '160px',
      flex: '1',
    }}>
      <div style={{ color: '#a6adc8', fontSize: '13px', marginBottom: '8px' }}>{label}</div>
      <div style={{ color: accent ?? '#cdd6f4', fontSize: '28px', fontWeight: '600' }}>
        {value ?? '—'}
      </div>
    </div>
  );
}

function Section({ title, children }) {
  return (
    <div style={{ marginBottom: '40px' }}>
      <h2 style={{ color: '#89b4fa', fontSize: '14px', marginBottom: '16px', letterSpacing: '0.05em', textTransform: 'uppercase' }}>
        {title}
      </h2>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '16px' }}>
        {children}
      </div>
    </div>
  );
}

export default function Dashboard() {
  const [jobMetrics, setJobMetrics] = useState(null);
  const [execMetrics, setExecMetrics] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchMetrics = () => {
      Promise.all([getJobMetrics(), getExecutionMetrics()])
        .then(([jobs, execs]) => {
          setJobMetrics(jobs);
          setExecMetrics(execs);
          setError(null);
        })
        .catch(() => setError('Failed to fetch metrics. Is the control plane running?'))
        .finally(() => setLoading(false));
    };

    fetchMetrics();
    const interval = setInterval(fetchMetrics, 5000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div style={{ padding: '40px', maxWidth: '1200px', margin: '0 auto' }}>

      <h1 style={{ color: '#cdd6f4', fontSize: '22px', marginBottom: '40px', fontWeight: '600' }}>
        Job Orchestration — System Dashboard
      </h1>

      {error && (
        <div style={{ color: '#f38ba8', marginBottom: '24px', fontSize: '14px' }}>{error}</div>
      )}

      {!loading && jobMetrics && (
        <Section title="Job Metrics">
          <MetricCard label="Total Jobs"  value={jobMetrics.totalJobs.toLocaleString()} />
          <MetricCard label="Completed"   value={jobMetrics.completedJobs.toLocaleString()}  accent="#a6e3a1" />
          <MetricCard label="Failed"      value={jobMetrics.failedJobs.toLocaleString()}     accent="#f38ba8" />
          <MetricCard label="In Progress" value={jobMetrics.inProgressJobs.toLocaleString()} accent="#f9e2af" />
        </Section>
      )}

      {!loading && execMetrics && (
        <Section title="Execution Metrics">
          <MetricCard label="Total Executions" value={execMetrics.totalExecutions.toLocaleString()} />
          <MetricCard label="Running"          value={execMetrics.runningExecutions.toLocaleString()}   accent="#f9e2af" />
          <MetricCard label="Completed"        value={execMetrics.completedExecutions.toLocaleString()} accent="#a6e3a1" />
          <MetricCard label="Failed"           value={execMetrics.failedExecutions.toLocaleString()}    accent="#f38ba8" />
          <MetricCard label="Timeout"          value={execMetrics.timeoutExecutions.toLocaleString()}   accent="#fab387" />
          <MetricCard
            label="Avg Execution Time"
            value={execMetrics.avgExecutionTimeMs > 0
              ? `${Math.round(execMetrics.avgExecutionTimeMs).toLocaleString()}ms`
              : '—'}
          />
        </Section>
      )}

      <div style={{ marginBottom: '16px' }}>
        <h2 style={{ color: '#89b4fa', fontSize: '14px', marginBottom: '16px', letterSpacing: '0.05em', textTransform: 'uppercase' }}>
          Jobs
        </h2>
        <div style={{ background: '#1e1e2e', border: '1px solid #313244', borderRadius: '8px', overflow: 'hidden' }}>
          <JobList />
        </div>
      </div>

    </div>
  );
}